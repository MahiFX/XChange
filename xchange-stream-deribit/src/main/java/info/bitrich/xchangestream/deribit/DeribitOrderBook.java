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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class DeribitOrderBook extends Observable<OrderBook> implements Consumer<DeribitMarketDataUpdateMessage> {
    private final CurrencyPair instrument;

    private final Subject<OrderBook> orderBookSubject = PublishSubject.<OrderBook>create().toSerialized();

    private final Map<BigDecimal, BigDecimal> bidPriceToBidQuantity = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    private final Map<BigDecimal, BigDecimal> offerPriceToOfferQuantity = new ConcurrentSkipListMap<>();

    public DeribitOrderBook(CurrencyPair instrument) {
        this.instrument = instrument;
    }

    @Override
    protected void subscribeActual(Observer<? super OrderBook> observer) {
        orderBookSubject.subscribe(observer);
    }

    @Override
    public void accept(DeribitMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
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
        List<LimitOrder> bids = new ArrayList<>(bidPriceToBidQuantity.size());
        List<LimitOrder> offers = new ArrayList<>(offerPriceToOfferQuantity.size());

        populateOrders(bids, bidPriceToBidQuantity, Order.OrderType.BID, instrument, timestamp);
        populateOrders(offers, offerPriceToOfferQuantity, Order.OrderType.ASK, instrument, timestamp);

        return new OrderBook(
                timestamp,
                offers,
                bids
        );
    }

    private void populateOrders(List<LimitOrder> orders, Map<BigDecimal, BigDecimal> priceToQuantity, Order.OrderType type, CurrencyPair currencyPair, Date timestamp) {
        for (Map.Entry<BigDecimal, BigDecimal> bigDecimalBigDecimalEntry : priceToQuantity.entrySet()) {
            BigDecimal price = bigDecimalBigDecimalEntry.getKey();
            BigDecimal quantity = bigDecimalBigDecimalEntry.getValue();

            LimitOrder order = new LimitOrder(type, quantity, currencyPair, null, timestamp, price);
            orders.add(order);
        }
    }

    private void handleSnapshot(DeribitMarketDataUpdateMessage deribitMarketDataUpdateMessage) {
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
