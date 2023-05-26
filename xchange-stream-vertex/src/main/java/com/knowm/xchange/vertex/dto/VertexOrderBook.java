package com.knowm.xchange.vertex.dto;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class VertexOrderBook extends Observable<OrderBook> implements Consumer<VertexMarketDataUpdateMessage> {
    private static final Logger logger = LoggerFactory.getLogger(VertexOrderBook.class);

    private final Subject<OrderBook> orderBookSubject = PublishSubject.<OrderBook>create().toSerialized();

    private final Map<BigInteger, BigInteger> bidPriceToBidQuantity = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final Map<BigInteger, BigInteger> offerPriceToOfferQuantity = new ConcurrentSkipListMap<>();
    private final Instrument instrument;
    private final int maxDepth;

    public VertexOrderBook(Instrument instrument, int maxDepth) {
        this.instrument = instrument;
        this.maxDepth = maxDepth;
    }

    @Override
    protected void subscribeActual(Observer<? super OrderBook> observer) {
        orderBookSubject.subscribe(observer);
    }

    @Override
    public synchronized void accept(VertexMarketDataUpdateMessage updateMessage) {
        if (updateMessage.getLastMaxTime() == null) {
            handleSnapshot(updateMessage);
        } else {
            handleIncrement(updateMessage);
        }

        publishOrderBookFromUpdate(updateMessage.getMaxTime());
    }

    private void processSnapshotOrders(Object[][] orders, Map<BigInteger, BigInteger> mapForInsert) {
        mapForInsert.clear();

        if (orders != null) {
            for (Object[] order : orders) {
                assert order.length == 3;

                BigInteger price = new BigInteger((String) order[0]);
                BigInteger quantity = new BigInteger((String) order[1]);

                mapForInsert.put(price, quantity);
            }
        }
    }

    private void handleIncrement(VertexMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
        processIncrementOrders(deribitMarketDataUpdateMessage.getBids(), bidPriceToBidQuantity);
        processIncrementOrders(deribitMarketDataUpdateMessage.getAsks(), offerPriceToOfferQuantity);
    }

    private void processIncrementOrders(Object[][] orders, Map<BigInteger, BigInteger> mapForInsert) {
        if (orders != null) {
            for (Object[] order : orders) {
                assert order.length == 3;


                BigInteger price = new BigInteger((String) order[0]);
                BigInteger quantity = new BigInteger((String) order[1]);

                if (quantity.equals(BigInteger.ZERO)) {
                    mapForInsert.remove(price);
                    break;
                } else {
                    mapForInsert.put(price, quantity);
                }

            }
        }
    }

    private void publishOrderBookFromUpdate(Instant timestamp) {
        OrderBook book = generateOrderBook(timestamp);

        orderBookSubject.onNext(book);
    }

    private void populateOrders(List<LimitOrder> orders, Map<BigInteger, BigInteger> priceToQuantity, Order.OrderType type, Instrument instrument, Date timestamp) {
        int currentDepth = 0;
        for (Map.Entry<BigInteger, BigInteger> bigDecimalBigDecimalEntry : priceToQuantity.entrySet()) {
            BigInteger priceInt = bigDecimalBigDecimalEntry.getKey();
            BigDecimal actualPrice = VertexModelUtils.convertToDecimal(priceInt);
            BigInteger quantityInt = bigDecimalBigDecimalEntry.getValue();
            BigDecimal actualQty = VertexModelUtils.convertToDecimal(quantityInt);

            LimitOrder order = new LimitOrder(type, actualQty, instrument, null, timestamp, actualPrice);
            orders.add(order);

            currentDepth += 1;

            if (currentDepth == maxDepth) break;
        }
    }

    private void handleSnapshot(VertexMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
        logger.info("Received snapshot for: {}. Clearing order book and repopulating.", instrument);
        processSnapshotOrders(deribitMarketDataUpdateMessage.getBids(), bidPriceToBidQuantity);
        processSnapshotOrders(deribitMarketDataUpdateMessage.getAsks(), offerPriceToOfferQuantity);
    }


    private OrderBook generateOrderBook(Instant instant) {
        int capacity = maxDepth != Integer.MAX_VALUE ? maxDepth : 50;
        List<LimitOrder> bids = new ArrayList<>(capacity);
        List<LimitOrder> offers = new ArrayList<>(capacity);

        Date timestamp = new Date(instant.toEpochMilli());
        populateOrders(bids, bidPriceToBidQuantity, Order.OrderType.BID, instrument, timestamp);
        populateOrders(offers, offerPriceToOfferQuantity, Order.OrderType.ASK, instrument, timestamp);

        return new OrderBook(
                new Date(),
                timestamp,
                offers,
                bids,
                false
        );
    }
}
