package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowm.xchange.vertex.dto.VertexMarketDataUpdateMessage;
import com.knowm.xchange.vertex.dto.VertexOrderBook;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class VertexStreamingMarketDataService implements StreamingMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(VertexStreamingMarketDataService.class);

    private final VertexStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;

    private final Map<CurrencyPair, Observable<OrderBook>> orderBookSubscriptions = new HashMap<>();
    private final Map<CurrencyPair, CompositeDisposable> orderBookDisposables = new HashMap<>();

    private final ObjectMapper mapper;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public VertexStreamingMarketDataService(VertexStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
        mapper = StreamingObjectMapperHelper.getObjectMapper();
        mapper.enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        final int maxDepth;
        if (args.length > 0 && args[0] instanceof Integer) {
            maxDepth = (int) args[0];
        } else {
            maxDepth = Integer.MAX_VALUE;
        }

        return orderBookSubscriptions.computeIfAbsent(
                currencyPair,
                c -> {

                    VertexOrderBook orderBook = new VertexOrderBook(c, maxDepth);

                    subscribeVertexOrderBook(c, orderBook);

                    return orderBook.doOnDispose(() -> {
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

    private void subscribeVertexOrderBook(CurrencyPair currencyPair, VertexOrderBook vertexOrderBook) {

        logger.info("Subscribing to orderBook for " + currencyPair);

        Disposable disconnectStreamDisposable = streamingService.subscribeDisconnect()
                .map(o -> {
                    logger.info("Clearing order book for {} due to disconnect: {}", currencyPair, o);
                    return VertexMarketDataUpdateMessage.empty();
                })
                .subscribe(vertexOrderBook, t -> {
                    throw new RuntimeException(t);
                });

        long productId = lookupProductId(currencyPair);

        String channelName = "book_depth." + productId;

        Observable<VertexMarketDataUpdateMessage> marketDataUpdateMessageObservable = streamingService.subscribeChannel(channelName, productId)
                .map(json -> {
                    VertexMarketDataUpdateMessage marketDataUpdate = mapper.treeToValue(json, VertexMarketDataUpdateMessage.class);

                    if (marketDataUpdate != null) {
                        return marketDataUpdate;
                    } else {
                        return VertexMarketDataUpdateMessage.NULL;
                    }
                });

        Disposable orderBookSubscription = marketDataUpdateMessageObservable
                .filter(update -> update != VertexMarketDataUpdateMessage.NULL)
                .subscribe(vertexOrderBook, t -> {
                    throw new RuntimeException(t);
                });

        AtomicReference<Instant> lastChangeId = new AtomicReference<>(null);
        Disposable reconnectOnIdSkip = marketDataUpdateMessageObservable.forEach(update -> {
            if (lastChangeId.get() == null || update.getLastMaxTime() == null) {
                lastChangeId.set(update.getMaxTime());
            } else {
                // compareAndSet relies on instance equality thanks to cache in NanoSecondsDeserializer
                if (!lastChangeId.compareAndSet(update.getLastMaxTime(), update.getMaxTime())) {
                    if (!lastChangeId.get().equals(update.getLastMaxTime())) {
                        logger.info("Unexpected gap in Change IDs for {} {} != {}. Reconnecting...", currencyPair, lastChangeId.get(), update.getLastMaxTime());
                        executor.schedule(
                                () -> disposeAndResubscribeOrderBook(currencyPair, vertexOrderBook),
                                10,
                                TimeUnit.MILLISECONDS
                        );
                    } else {
                        lastChangeId.set(update.getMaxTime());
                    }
                }
            }
        });

        CompositeDisposable compositeDisposableForPair = orderBookDisposables.computeIfAbsent(currencyPair, cp -> new CompositeDisposable());
        compositeDisposableForPair.add(disconnectStreamDisposable);
        compositeDisposableForPair.add(orderBookSubscription);
        compositeDisposableForPair.add(reconnectOnIdSkip);
    }

    private long lookupProductId(CurrencyPair currencyPair) {
        switch (currencyPair.toString()) {
            case "WBTC/USDC":
                return 1;
            case "WETH/USDC":
                return 3;
            default:
                throw new RuntimeException("unknown product id for " + currencyPair);
        }
        //FIXME lookup via REST API

    }

    private void disposeAndResubscribeOrderBook(CurrencyPair instrument, VertexOrderBook orderBook) {
        CompositeDisposable disposables = orderBookDisposables.remove(instrument);
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
        } else {
            logger.info("Unexpected! CompositeDisposable: {} for {} was null or already disposed...", disposables, instrument);
        }

        // Clear order book
        logger.info("Clearing order book for {} before reconnect", instrument);
        orderBook.accept(VertexMarketDataUpdateMessage.empty());

        // Schedule reconnection
        executor.schedule(() -> subscribeVertexOrderBook(instrument, orderBook), 500, TimeUnit.MILLISECONDS);
    }

}
