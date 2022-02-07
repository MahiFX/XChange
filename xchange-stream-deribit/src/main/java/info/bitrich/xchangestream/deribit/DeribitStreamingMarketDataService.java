package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.deribit.dto.DeribitMarketDataUpdateMessage;
import info.bitrich.xchangestream.deribit.dto.DeribitTradeData;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static info.bitrich.xchangestream.deribit.DeribitStreamingUtil.instrumentName;
import static info.bitrich.xchangestream.deribit.DeribitStreamingUtil.tryGetDataAsType;

public class DeribitStreamingMarketDataService implements StreamingMarketDataService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingMarketDataService.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final DeribitStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;

    private final Map<CurrencyPair, Observable<OrderBook>> orderbookSubscriptions = Maps.newHashMap();
    private final Map<CurrencyPair, Observable<Trade>> tradeSubscriptions = Maps.newHashMap();

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingMarketDataService(DeribitStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        return orderbookSubscriptions.computeIfAbsent(
                currencyPair,
                c -> {
                    authenticate();

                    DeribitOrderBook orderBook = new DeribitOrderBook(c);

                    setupOrderBookSubscriptions(c, orderBook);

                    return orderBook.share();
                }
        );
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        return tradeSubscriptions.computeIfAbsent(
                currencyPair,
                cPair1 -> {
                    authenticate();

                    String channelName = "trades." + instrumentName(currencyPair) + ".raw";

                    logger.debug("Subscribing to trade channel: " + channelName);

                    Observable<DeribitTradeData[]> tradeData = streamingService.subscribeChannel(channelName)
                            .map(json -> {
                                DeribitTradeData[] deribitTradeData = tryGetDataAsType(mapper, json, DeribitTradeData[].class);

                                if (deribitTradeData != null) {
                                    return deribitTradeData;
                                } else {
                                    return new DeribitTradeData[0];
                                }
                            });

                    return tradeData.flatMapIterable(dtdArr -> {
                        List<Trade> trades = new ArrayList<>();

                        for (DeribitTradeData deribitTradeData : dtdArr) {
                            trades.add(deribitTradeData.toTrade(currencyPair));
                        }

                        return trades;
                    }).share();
                }
        );
    }

    private void authenticate() {
        try {
            streamingService.authenticate(exchangeSpecification.getApiKey(), exchangeSpecification.getSecretKey());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void setupOrderBookSubscriptions(CurrencyPair currencyPair, DeribitOrderBook orderBook) {
        String channelName = "book." + instrumentName(currencyPair) + ".raw";

        logger.debug("Subscribing to orderBook channel: " + channelName);

        Disposable disconnectStreamDisposable = streamingService.subscribeDisconnect()
                .map(o -> {
                    logger.debug("Clearing order book for {} due to disconnect: {}", currencyPair, o);
                    return DeribitMarketDataUpdateMessage.empty(new Date());
                })
                .subscribe(orderBook, t -> {
                    throw new RuntimeException(t);
                });

        Observable<DeribitMarketDataUpdateMessage> marketDataUpdateMessageObservable = streamingService.subscribeChannel(channelName)
                .map(json -> {
                    DeribitMarketDataUpdateMessage marketDataUpdate = tryGetDataAsType(mapper, json, DeribitMarketDataUpdateMessage.class);

                    if (marketDataUpdate != null) {
                        return marketDataUpdate;
                    } else {
                        return DeribitMarketDataUpdateMessage.NULL;
                    }
                });

        Disposable orderBookSubscriptionDisposable = marketDataUpdateMessageObservable
                .filter(update -> update != DeribitMarketDataUpdateMessage.NULL)
                .subscribe(orderBook, t -> {
                    throw new RuntimeException(t);
                });

        AtomicLong lastChangeId = new AtomicLong(-1);
        Disposable safeToIgnore = marketDataUpdateMessageObservable.forEach(update -> {
            if (lastChangeId.get() == -1 || update.getPrevChangeId() == null) {
                lastChangeId.set(update.getChangeId());
            } else {
                if (!lastChangeId.compareAndSet(update.getPrevChangeId(), update.getChangeId())) {
                    logger.debug("Unexpected gap in Change IDs for {}. Reconnecting...", currencyPair);
                    executor.schedule(
                            () -> closeAndReconnectOrderBook(
                                    currencyPair,
                                    orderBook,
                                    disconnectStreamDisposable,
                                    orderBookSubscriptionDisposable),
                            10,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        });
    }

    private void closeAndReconnectOrderBook(CurrencyPair instrument, DeribitOrderBook orderBook, Disposable... disposables) {
        // Dispose disposables
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        // Clear order book
        logger.debug("Clearing order book for {} before reconnect", instrument);
        orderBook.accept(DeribitMarketDataUpdateMessage.empty(new Date()));

        // Schedule reconnection
        executor.schedule(() -> {
            setupOrderBookSubscriptions(instrument, orderBook);
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }
}
