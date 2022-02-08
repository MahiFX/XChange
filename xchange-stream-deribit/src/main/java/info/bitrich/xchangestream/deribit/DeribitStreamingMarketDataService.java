package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.deribit.dto.DeribitMarketDataUpdateMessage;
import info.bitrich.xchangestream.deribit.dto.DeribitTradeData;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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

    private final Map<CurrencyPair, Observable<OrderBook>> orderBookSubscriptions = new HashMap<>();
    private final Map<CurrencyPair, CompositeDisposable> orderBookDisposables = new HashMap<>();

    private final Map<CurrencyPair, Observable<Trade>> tradeSubscriptions = new HashMap<>();

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingMarketDataService(DeribitStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        return orderBookSubscriptions.computeIfAbsent(
                currencyPair,
                c -> {
                    authenticate();

                    DeribitOrderBook orderBook = new DeribitOrderBook(c);

                    subscribeDeribitOrderBook(c, orderBook);

                    return orderBook
                            .doOnDispose(() -> {
                                CompositeDisposable compositeDisposable = orderBookDisposables.remove(c);
                                if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
                                    compositeDisposable.dispose();
                                }

                                orderBookSubscriptions.remove(c);
                            })
                            .share();
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

                    logger.info("Subscribing to trade channel: " + channelName);

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

    private void subscribeDeribitOrderBook(CurrencyPair currencyPair, DeribitOrderBook deribitOrderBook) {
        String channelName = "book." + instrumentName(currencyPair) + ".raw";

        logger.info("Subscribing to orderBook channel: " + channelName);

        Disposable disconnectStreamDisposable = streamingService.subscribeDisconnect()
                .map(o -> {
                    logger.info("Clearing order book for {} due to disconnect: {}", currencyPair, o);
                    return DeribitMarketDataUpdateMessage.empty(new Date());
                })
                .subscribe(deribitOrderBook, t -> {
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

        Disposable orderBookSubscription = marketDataUpdateMessageObservable
                .filter(update -> update != DeribitMarketDataUpdateMessage.NULL)
                .subscribe(deribitOrderBook, t -> {
                    throw new RuntimeException(t);
                });

        AtomicLong lastChangeId = new AtomicLong(-1);
        Disposable reconnectOnIdSkip = marketDataUpdateMessageObservable.forEach(update -> {
            if (lastChangeId.get() == -1 || update.getPrevChangeId() == null) {
                lastChangeId.set(update.getChangeId());
            } else {
                if (!lastChangeId.compareAndSet(update.getPrevChangeId(), update.getChangeId())) {
                    logger.info("Unexpected gap in Change IDs for {}. Reconnecting...", currencyPair);
                    executor.schedule(
                            () -> disposeAndResubscribeOrderBook(currencyPair, deribitOrderBook),
                            10,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        });

        CompositeDisposable compositeDisposableForPair = orderBookDisposables.computeIfAbsent(currencyPair, cp -> new CompositeDisposable());
        compositeDisposableForPair.add(disconnectStreamDisposable);
        compositeDisposableForPair.add(orderBookSubscription);
        compositeDisposableForPair.add(reconnectOnIdSkip);
    }

    private void disposeAndResubscribeOrderBook(CurrencyPair instrument, DeribitOrderBook orderBook) {
        CompositeDisposable disposables = orderBookDisposables.remove(instrument);
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
        } else {
            logger.info("Unexpected! CompositeDisposable: {} for {} was null or already disposed...", disposables, instrument);
        }

        // Clear order book
        logger.info("Clearing order book for {} before reconnect", instrument);
        orderBook.accept(DeribitMarketDataUpdateMessage.empty(new Date()));

        // Schedule reconnection
        executor.schedule(() -> {
            subscribeDeribitOrderBook(instrument, orderBook);
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }
}
