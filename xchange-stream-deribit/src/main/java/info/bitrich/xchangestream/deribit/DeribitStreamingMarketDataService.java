package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.deribit.dto.DeribitMarketDataUpdateMessage;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DeribitStreamingMarketDataService implements StreamingMarketDataService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingMarketDataService.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final DeribitStreamingService streamingService;

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingMarketDataService(DeribitStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        DeribitOrderBook orderBook = new DeribitOrderBook(currencyPair);

        setupOrderBookSubscriptions(currencyPair, orderBook);

        return orderBook;
    }

    private void setupOrderBookSubscriptions(CurrencyPair currencyPair, DeribitOrderBook orderBook) {
        String channelName = "book." + currencyPair.toString().replace("/", "-") + ".raw";

        logger.debug("Subscribing to orderBook channel: " + channelName);

        Disposable disconnectStreamDisposable = streamingService.subscribeDisconnect()
                .map(o -> {
                    logger.debug("Clearing order book for {} due to disconnect: {}", currencyPair, o);
                    return DeribitMarketDataUpdateMessage.EMPTY;
                })
                .subscribe(orderBook, t -> {
                    throw new RuntimeException(t);
                });

        Observable<DeribitMarketDataUpdateMessage> marketDataUpdateMessageObservable = streamingService.subscribeChannel(channelName)
                .map(json -> {
                    if (json.has("params")) {
                        JsonNode params = json.get("params");
                        if (params.has("data")) {
                            JsonNode data = params.get("data");

                            return mapper.treeToValue(data, DeribitMarketDataUpdateMessage.class);
                        }
                    }

                    return DeribitMarketDataUpdateMessage.EMPTY;
                });

        Disposable orderBookSubscriptionDisposable = marketDataUpdateMessageObservable
                .filter(update -> update != DeribitMarketDataUpdateMessage.EMPTY)
                .subscribe(orderBook, t -> {
                    throw new RuntimeException(t);
                });

        AtomicLong lastChangeId = new AtomicLong(-1);
        Disposable safeToIgnore = marketDataUpdateMessageObservable.forEach(update -> {
            if (lastChangeId.get() == -1) {
                lastChangeId.set(update.getChangeId());
            } else {
                if (!lastChangeId.compareAndSet(update.getPrevChangeId(), update.getChangeId())) {
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
        orderBook.accept(DeribitMarketDataUpdateMessage.EMPTY);

        // Schedule reconnection
        executor.schedule(() -> {
            setupOrderBookSubscriptions(instrument, orderBook);
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }
}
