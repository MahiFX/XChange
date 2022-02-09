package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.deribit.dto.DeribitMarketDataUpdateMessage;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class DeribitOrderBook extends Observable<OrderBook> implements Consumer<DeribitMarketDataUpdateMessage> {
    private static final Logger logger = LoggerFactory.getLogger(DeribitOrderBook.class);

    private final CurrencyPair instrument;
    private final int depthLimit;

    private final Subject<OrderBook> orderBookSubject = PublishSubject.<OrderBook>create().toSerialized();

    private final Map<BigDecimal, BigDecimal> bidPriceToBidQuantity = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final Map<BigDecimal, BigDecimal> offerPriceToOfferQuantity = new ConcurrentSkipListMap<>();

    public DeribitOrderBook(CurrencyPair instrument, int depthLimit) {
        this.instrument = instrument;
        this.depthLimit = depthLimit;
    }

    @Override
    protected void subscribeActual(Observer<? super OrderBook> observer) {
        orderBookSubject.subscribe(observer);
    }

    @Override
    public synchronized void accept(DeribitMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
        if (deribitMarketDataUpdateMessage.getPrevChangeId() == null) {
            handleSnapshot(deribitMarketDataUpdateMessage);
        } else {
            handleIncrement(deribitMarketDataUpdateMessage);
        }

        publishOrderBookFromUpdate(deribitMarketDataUpdateMessage.getTimestamp());
    }

    private void publishOrderBookFromUpdate(Date timestamp) {
        OrderBook book = generateOrderBook(timestamp);

        orderBookSubject.onNext(book);
    }

    private OrderBook generateOrderBook(Date timestamp) {
        List<LimitOrder> bids = new ArrayList<>(depthLimit);
        List<LimitOrder> offers = new ArrayList<>(depthLimit);

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

    private void populateOrders(List<LimitOrder> orders, Map<BigDecimal, BigDecimal> priceToQuantity, Order.OrderType type, CurrencyPair currencyPair, Date timestamp) {
        int currentDepth = 0;
        for (Map.Entry<BigDecimal, BigDecimal> bigDecimalBigDecimalEntry : priceToQuantity.entrySet()) {
            BigDecimal price = bigDecimalBigDecimalEntry.getKey();
            BigDecimal quantity = bigDecimalBigDecimalEntry.getValue();

            LimitOrder order = new LimitOrder(type, quantity, currencyPair, null, timestamp, price);
            orders.add(order);

            currentDepth += 1;

            if (currentDepth == depthLimit) break;
        }
    }

    private void handleSnapshot(DeribitMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
        logger.info("Received snapshot for: {}. Clearing order book and repopulating.", instrument);
        processSnapshotOrders(deribitMarketDataUpdateMessage.getBids(), bidPriceToBidQuantity);
        processSnapshotOrders(deribitMarketDataUpdateMessage.getAsks(), offerPriceToOfferQuantity);
    }

    private void processSnapshotOrders(Object[][] orders, Map<BigDecimal, BigDecimal> mapForInsert) {
        mapForInsert.clear();

        if (orders != null) {
            for (Object[] order : orders) {
                assert order.length == 3;

                BigDecimal price = (BigDecimal) order[1];
                BigDecimal quantity = (BigDecimal) order[2];

                mapForInsert.put(price, quantity);
            }
        }
    }

    private void handleIncrement(DeribitMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
        processIncrementOrders(deribitMarketDataUpdateMessage.getBids(), bidPriceToBidQuantity);
        processIncrementOrders(deribitMarketDataUpdateMessage.getAsks(), offerPriceToOfferQuantity);
    }

    private void processIncrementOrders(Object[][] orders, Map<BigDecimal, BigDecimal> mapForInsert) {
        if (orders != null) {
            for (Object[] order : orders) {
                assert order.length == 3;

                String action = (String) order[0];
                BigDecimal price = (BigDecimal) order[1];
                BigDecimal quantity = (BigDecimal) order[2];

                switch (action) {
                    case "delete":
                        mapForInsert.remove(price);
                        break;
                    case "change":
                        BigDecimal oldValue = mapForInsert.put(price, quantity);
                        assert oldValue != null;
                        break;
                    case "new":
                        BigDecimal expectNull = mapForInsert.put(price, quantity);
                        assert expectNull == null;
                        break;
                    default:
                        throw new RuntimeException("Unexpected order action: " + action);
                }
            }
        }
    }
}
