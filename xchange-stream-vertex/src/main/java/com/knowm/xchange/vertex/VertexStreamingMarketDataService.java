package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knowm.xchange.vertex.dto.PriceAndQuantity;
import com.knowm.xchange.vertex.dto.VertexMarketDataUpdateMessage;
import com.knowm.xchange.vertex.dto.VertexOrderBookStream;
import com.knowm.xchange.vertex.dto.VertexTradeData;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class VertexStreamingMarketDataService implements StreamingMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(VertexStreamingMarketDataService.class);
    public static final int DEFAULT_DEPTH = 20;
    public static final TypeReference<List<PriceAndQuantity>> PRICE_LIST_TYPE_REF = new TypeReference<>() {
    };

    private final VertexStreamingService subscriptionStream;

    private final Map<Instrument, Observable<VertexMarketDataUpdateMessage>> orderBooksStreams = new ConcurrentHashMap<>();

    private final Map<Instrument, Observable<Trade>> tradeSubscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    private final VertexProductInfo productInfo;
    private final VertexStreamingExchange exchange;
    private final JavaType PRICE_LIST_TYPE;


    public VertexStreamingMarketDataService(VertexStreamingService subscriptionStream, VertexProductInfo productInfo, VertexStreamingExchange vertexStreamingExchange) {
        this.subscriptionStream = subscriptionStream;
        this.productInfo = productInfo;
        this.exchange = vertexStreamingExchange;
        mapper = StreamingObjectMapperHelper.getObjectMapper();
        mapper.enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        mapper.registerModule(new JavaTimeModule());
        PRICE_LIST_TYPE = mapper.constructType(PRICE_LIST_TYPE_REF.getType());
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        //noinspection UnnecessaryLocalVariable
        Instrument inst = currencyPair;
        return this.getOrderBook(inst, args);
    }

    @Override
    public Observable<OrderBook> getOrderBook(Instrument instrument, Object... args) {
        final int maxDepth;
        if (args.length > 0 && args[0] instanceof Integer) {
            maxDepth = (int) args[0];
        } else {
            maxDepth = DEFAULT_DEPTH;
        }

        long productId = productInfo.lookupProductId(instrument);

        OrderBookSubscription sub = new OrderBookSubscription(instrument, maxDepth);

        Observable<VertexMarketDataUpdateMessage> cachedStream = orderBooksStreams.computeIfAbsent(
                instrument,
                newInstrument -> {
                    logger.info("Subscribing to orderBook for " + newInstrument);

                    String channelName = "book_depth." + productInfo.lookupProductId(newInstrument);

                    Observable<VertexMarketDataUpdateMessage> marketDataUpdates = subscriptionStream.subscribeChannel(channelName)
                            .map(json -> {
                                VertexMarketDataUpdateMessage marketDataUpdate = mapper.treeToValue(json, VertexMarketDataUpdateMessage.class);

                                return Objects.requireNonNullElse(marketDataUpdate, VertexMarketDataUpdateMessage.EMPTY);
                            });

                    Observable<VertexMarketDataUpdateMessage> clearOnDisconnect = subscriptionStream.subscribeDisconnect()
                            .map(o -> {
                                logger.info("Clearing order books for {} due to disconnect: {}", newInstrument, o);
                                return VertexMarketDataUpdateMessage.EMPTY;
                            });


                    AtomicReference<Instant> lastChangeId = new AtomicReference<>(null);


                    Observable<VertexMarketDataUpdateMessage> reconnectOnIdSkip = marketDataUpdates.filter(update -> {
                        if (lastChangeId.get() == null || update.getLastMaxTime() == null) {
                            lastChangeId.set(update.getMaxTime());
                        } else {
                            // compareAndSet relies on instance equality thanks to cache in NanoSecondsDeserializer
                            if (!lastChangeId.compareAndSet(update.getLastMaxTime(), update.getMaxTime())) {
                                if (!lastChangeId.get().equals(update.getLastMaxTime())) {
                                    logger.error("Unexpected gap in timestamps for {} {} != {}. Resubscribing...", sub.getInstrument(), lastChangeId.get(), update.getLastMaxTime());
                                    return true;

                                } else {
                                    lastChangeId.set(update.getMaxTime());
                                }
                            }
                        }
                        return false;
                    }).observeOn(Schedulers.io()).map(badUpdate -> {
                        subscriptionStream.sendUnsubscribeMessage(channelName);
                        CountDownLatch latch = new CountDownLatch(1);
                        AtomicReference<VertexMarketDataUpdateMessage> snapshotHolder = new AtomicReference<>();
                        exchange.submitQueries(new Query(snapshotQuery(maxDepth, productId), (data) -> {
                            subscriptionStream.sendSubscribeMessage(channelName);
                            VertexMarketDataUpdateMessage snapshot = buildSnapshotFromQueryResponse(productId, data);
                            snapshotHolder.set(snapshot);
                            latch.countDown();
                        }));
                        if (!latch.await(10, TimeUnit.SECONDS)) {
                            throw new RuntimeException("Timed out waiting for snapshot");
                        }
                        return snapshotHolder.get();
                    });

                    return Observable.merge(
                                    marketDataUpdates,
                                    clearOnDisconnect,
                                    reconnectOnIdSkip
                            )
                            .share();
                }
        );


        VertexOrderBookStream instrumentAndDepthStream = new VertexOrderBookStream(instrument, maxDepth);

        AtomicReference<Instant> snapshotTimeHolder = new AtomicReference<>();

        //Subscribe to updates but drop until snapshot reply - there is still a chance we could miss a message but not much we can do about that
        Disposable instrumentStream = cachedStream.subscribe(update -> {
            Instant snapshotTime = snapshotTimeHolder.get();
            if (snapshotTime == null || update.getMaxTime().isAfter(snapshotTime)) {
                if (snapshotTime != null) {
                    snapshotTimeHolder.set(null);
                }
                instrumentAndDepthStream.accept(update);
            }

        }, t -> {
            throw new RuntimeException(t);
        });


        // Request snapshot for new subscriber
        exchange.submitQueries(new Query(snapshotQuery(maxDepth, productId), (data) -> {
            VertexMarketDataUpdateMessage snapshot = buildSnapshotFromQueryResponse(productId, data);
            snapshotTimeHolder.set(snapshot.getMaxTime());
            instrumentAndDepthStream.accept(snapshot);
        }));


        return instrumentAndDepthStream.doOnDispose(instrumentStream::dispose);
    }

    private VertexMarketDataUpdateMessage buildSnapshotFromQueryResponse(long productId, JsonNode data) {
        return new VertexMarketDataUpdateMessage(parsePrices(data, "bids"), parsePrices(data, "asks"), NanoSecondsDeserializer.parse(data.get("timestamp").asText()), NanoSecondsDeserializer.parse(data.get("timestamp").asText()), null, productId);
    }

    private static String snapshotQuery(int maxDepth, long productId) {
        return "{\"type\":\"market_liquidity\",\"product_id\": " + productId + ", \"depth\": " + maxDepth + "}";
    }

    private List<PriceAndQuantity> parsePrices(JsonNode data, String field) {
        ArrayNode bidArray = data.withArray(field);

        List<PriceAndQuantity> bids;
        try {
            bids = mapper.treeToValue(bidArray, PRICE_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return bids;
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair instrument, Object... args) {
        //noinspection UnnecessaryLocalVariable
        Instrument inst = instrument;
        return getTrades(inst, args);
    }

    @Override
    public Observable<Trade> getTrades(Instrument instrument, Object... args) {

        return tradeSubscriptions.computeIfAbsent(
                instrument,
                newInstrument -> {

                    String channelName = "trade." + productInfo.lookupProductId(instrument);

                    logger.info("Subscribing to trade channel: " + channelName);

                    return subscriptionStream.subscribeChannel(channelName)
                            .map(json -> mapper.treeToValue(json, VertexTradeData.class).toTrade(instrument));


                }).share();
    }

    @Getter
    private class OrderBookSubscription {
        private final Instrument instrument;
        private final int maxDepth;
        private final long productId;

        public OrderBookSubscription(Instrument instrument, int maxDepth) {
            this.instrument = instrument;
            this.maxDepth = maxDepth;
            this.productId = productInfo.lookupProductId(instrument);
        }
    }


}
