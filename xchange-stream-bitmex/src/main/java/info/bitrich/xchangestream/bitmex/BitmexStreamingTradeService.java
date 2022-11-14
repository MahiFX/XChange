package info.bitrich.xchangestream.bitmex;

import info.bitrich.xchangestream.bitmex.dto.BitmexOrder;
import info.bitrich.xchangestream.core.StreamingTradeService;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.knowm.xchange.bitmex.BitmexAdapters;
import org.knowm.xchange.bitmex.dto.trade.BitmexPrivateExecution;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.UserTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by Declan
 */
public class BitmexStreamingTradeService implements StreamingTradeService {
    private static final Logger LOG = LoggerFactory.getLogger(BitmexStreamingTradeService.class);

    private static final String ORDER_CHANGES_CHANNEL_NAME = "order";
    private static final String USER_TRADES_CHANNEL_NAME = "execution";

    private final Subject<BitmexOrder> orderChangesPublisher = PublishSubject.<BitmexOrder>create().toSerialized();
    private final AtomicBoolean ordersSubscribed = new AtomicBoolean(false);

    private final Subject<BitmexPrivateExecution> userTradesPublisher = PublishSubject.<BitmexPrivateExecution>create().toSerialized();
    private final AtomicBoolean userTradesSubscribed = new AtomicBoolean(false);

    private final BitmexStreamingService streamingService;

    public BitmexStreamingTradeService(BitmexStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    private void startOrdersSubscription() {
        LOG.info("Starting BitMEX orders subscription");
        streamingService.subscribeBitmexChannel(ORDER_CHANGES_CHANNEL_NAME)
                .flatMapIterable(
                        s -> {
                            BitmexOrder[] bitmexOrders = s.toBitmexOrders();
                            return Arrays.stream(bitmexOrders)
                                    .filter(BitmexOrder::isNotWorkingIndicator)
                                    .collect(Collectors.toList());
                        })
                .subscribe(orderChangesPublisher::onNext);
    }

    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        if (ordersSubscribed.compareAndSet(false, true)) {
            startOrdersSubscription();
        }

        String instrument = currencyPair.base.toString() + currencyPair.counter.toString();
        return orderChangesPublisher
                .filter(bitmexOrder -> bitmexOrder.getSymbol().equals(instrument))
                .map(BitmexOrder::toOrder);
    }

    private void startUserTradesSubscription() {
        LOG.info("Starting BitMEX execution subscription");
        streamingService.subscribeBitmexChannel(USER_TRADES_CHANNEL_NAME)
                .flatMapIterable(
                        s -> {
                            BitmexPrivateExecution[] bitmexExecutions = s.toBitmexExecutions();
                            return Arrays.stream(bitmexExecutions)
                                    .collect(Collectors.toList());
                        })
                .filter(e -> "Trade".equals(e.execType))
                .subscribe(userTradesPublisher::onNext);
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        if (userTradesSubscribed.compareAndSet(false, true)) {
            startUserTradesSubscription();
        }

        String instrument = currencyPair.base.toString() + currencyPair.counter.toString();
        return userTradesPublisher
                .filter(execution -> execution.symbol.equals(instrument))
                .map(BitmexAdapters::adoptUserTrade);
    }
}
