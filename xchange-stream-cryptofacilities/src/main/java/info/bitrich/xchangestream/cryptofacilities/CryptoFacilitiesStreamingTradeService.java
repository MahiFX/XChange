package info.bitrich.xchangestream.cryptofacilities;

import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesFill;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesFillsMessage;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesOpenOrder;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesOpenOrdersMessage;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesSubscriptionName;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchange.cryptofacilities.CryptoFacilitiesAdapters;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.dto.trade.UserTrade;

import java.math.BigDecimal;
import java.util.*;

public class CryptoFacilitiesStreamingTradeService implements StreamingTradeService {
    private final CryptoFacilitiesStreamingService streamingService;

    private Observable<Order> openOrdersObservable;
    private Observable<UserTrade> fillsObservable;

    CryptoFacilitiesStreamingTradeService(CryptoFacilitiesStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    private String getChannelName(CryptoFacilitiesSubscriptionName subscriptionName) {
        return subscriptionName.toString();
    }

    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        try {
            if (openOrdersObservable == null) {
                synchronized (this) {
                    if (openOrdersObservable == null) {
                        String channelName = getChannelName(CryptoFacilitiesSubscriptionName.open_orders_verbose);
                        openOrdersObservable =
                                streamingService
                                        .subscribeChannel(channelName)
                                        .filter(Objects::nonNull)
                                        .map(
                                                jsonNode ->
                                                        StreamingObjectMapperHelper.getObjectMapper()
                                                                .treeToValue(jsonNode, CryptoFacilitiesOpenOrdersMessage.class))
                                        .flatMapIterable(this::adaptCryptoFacilitiesOrders)
                                        .share();
                    }
                }
            }
            return Observable.create(
                    emit ->
                            openOrdersObservable
                                    .filter(
                                            order ->
                                                    currencyPair == null
                                                            || order.getCurrencyPair() == null
                                                            || order.getCurrencyPair().compareTo(currencyPair) == 0)
                                    .subscribe(emit::onNext, emit::onError, emit::onComplete));

        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        try {
            if (fillsObservable == null) {
                synchronized (this) {
                    if (fillsObservable == null) {
                        String channelName = getChannelName(CryptoFacilitiesSubscriptionName.fills);
                        fillsObservable =
                                streamingService
                                        .subscribeChannel(channelName)
                                        .filter(Objects::nonNull)
                                        .map(
                                                jsonNode ->
                                                        StreamingObjectMapperHelper.getObjectMapper()
                                                                .treeToValue(jsonNode, CryptoFacilitiesFillsMessage.class))
                                        .flatMapIterable(this::adaptCryptoFacilitiesFills)
                                        .share();
                    }
                }
            }
            return Observable.create(
                    emit ->
                            fillsObservable
                                    .filter(
                                            order ->
                                                    currencyPair == null
                                                            || order.getCurrencyPair() == null
                                                            || order.getCurrencyPair().compareTo(currencyPair) == 0)
                                    .subscribe(emit::onNext, emit::onError, emit::onComplete));

        } catch (Exception e) {
            return Observable.error(e);
        }
    }

    private Iterable<Order> adaptCryptoFacilitiesOrders(CryptoFacilitiesOpenOrdersMessage message) {
        CryptoFacilitiesOpenOrder[] orders = message.getOrders();

        List<Order> adaptedOrders = new ArrayList<>();

        if (ArrayUtils.isEmpty(orders)) {
            if (Boolean.TRUE.equals(message.getCancel())) {
                adaptedOrders.add(new CryptoFacilitiesSimpleCancelOrder(
                        message.getOrderId(),
                        new Date()
                ));

                return adaptedOrders;
            } else {
                return Collections.emptyList();
            }
        }

        for (CryptoFacilitiesOpenOrder order : orders) {
            String orderId = order.getOrderId();

            CurrencyPair pair = CryptoFacilitiesAdapters.adaptCurrencyPair(order.getInstrument());
            Order.OrderType side = CryptoFacilitiesStreamingAdapters.adaptOrderType(order.getSide());
            String orderType = order.getOrderType();

            Order.Builder builder;
            if ("limit".equals(orderType))
                builder = new LimitOrder.Builder(side, pair).limitPrice(order.getLimitPrice());
            else if ("stop".equals(orderType))
                builder = new StopOrder.Builder(side, pair).limitPrice(order.getLimitPrice()).stopPrice(order.getStopPrice());
            else // this is an order update (not the full order, it may only update one field)
                throw new IllegalArgumentException("Unsupported type " + orderType);

            adaptedOrders.add(
                    builder
                            .id(orderId)
                            .originalAmount(order.getSize())
                            .cumulativeAmount(order.getFilledSize())
                            .orderStatus(adaptOrderStatus(message, order))
                            .timestamp(order.getTime())
                            .userReference(order.getClientOrderId())
                            .build());
        }

        return adaptedOrders;
    }

    private Iterable<UserTrade> adaptCryptoFacilitiesFills(CryptoFacilitiesFillsMessage cryptoFacilitiesFillsMessage) {
        List<UserTrade> result = new ArrayList<>();

        for (CryptoFacilitiesFill fill : cryptoFacilitiesFillsMessage.getFills()) {
            CurrencyPair pair = CryptoFacilitiesAdapters.adaptCurrencyPair(fill.getInstrument());

            result.add(
                    new UserTrade.Builder()
                            .id(fill.getFillId())
                            .orderId(fill.getOrderId())
                            .currencyPair(pair)
                            .timestamp(fill.getTime())
                            .type(fill.isBuy() ? Order.OrderType.BID : Order.OrderType.ASK)
                            .price(fill.getPrice())
                            .feeAmount(fill.getFeePaid())
                            .feeCurrency(Currency.getInstance(fill.getFeeCurrency()))
                            .originalAmount(fill.getSize())
                            .build());
        }
        return result;
    }

    private Order.OrderStatus adaptOrderStatus(CryptoFacilitiesOpenOrdersMessage message, CryptoFacilitiesOpenOrder order) {
        if (!Boolean.TRUE.equals(message.getCancel())) {
            return order.getFilledSize().compareTo(BigDecimal.ZERO) > 0 ? Order.OrderStatus.PARTIALLY_FILLED : Order.OrderStatus.NEW;
        } else if (order.getFilledSize().compareTo(order.getSize()) == 0) {
            return Order.OrderStatus.FILLED;
        } else if (order.getFilledSize().compareTo(BigDecimal.ZERO) == 0) {
            return Order.OrderStatus.CANCELED;
        } else {
            return Order.OrderStatus.PARTIALLY_FILLED;
        }
    }
}
