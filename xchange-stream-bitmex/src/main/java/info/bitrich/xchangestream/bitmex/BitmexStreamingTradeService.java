package info.bitrich.xchangestream.bitmex;

import info.bitrich.xchangestream.bitmex.dto.BitmexOrder;
import info.bitrich.xchangestream.core.StreamingTradeService;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by Declan
 */
public class BitmexStreamingTradeService implements StreamingTradeService {
    private static final String ORDER_CHANGES_CHANNEL_NAME = "order";

    private final Subject<BitmexOrder> orderChangesPublisher = PublishSubject.<BitmexOrder>create().toSerialized();
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    private final BitmexStreamingService streamingService;

    public BitmexStreamingTradeService(BitmexStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    public void startSubscription() {
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
        if (subscribed.getAndSet(true)) {
            startSubscription();
        }

        String instrument = currencyPair.base.toString() + currencyPair.counter.toString();
        return orderChangesPublisher
                .filter(bitmexOrder -> bitmexOrder.getSymbol().equals(instrument))
                .map(BitmexOrder::toOrder);
    }
}
