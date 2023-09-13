package org.knowm.xchange.binance.futures.service;

import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.futures.BinanceFuturesAdapters;
import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.binance.futures.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.futures.dto.BinancePosition;
import org.knowm.xchange.binance.futures.dto.OrderType;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.account.OpenPositions;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.CancelOrderByCurrencyPair;
import org.knowm.xchange.service.trade.params.CancelOrderParams;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParamCurrencyPair;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;
import org.knowm.xchange.service.trade.params.orders.OrderQueryParamCurrencyPair;
import org.knowm.xchange.service.trade.params.orders.OrderQueryParams;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BinanceFuturesTradeService extends BinanceFuturesTradeServiceRaw implements TradeService {
    public BinanceFuturesTradeService(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, binanceFutures, resilienceRegistries);
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        return placeOrder(OrderType.MARKET, marketOrder, null, null);
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {
        return placeOrder(OrderType.LIMIT, limitOrder, limitOrder.getLimitPrice(), null);
    }

    @Override
    public String placeStopOrder(StopOrder stopOrder) throws IOException {
        return placeOrder(OrderType.STOP, stopOrder, null, stopOrder.getStopPrice());
    }

    private String placeOrder(OrderType type, Order order, BigDecimal limitPrice, BigDecimal stopPrice) throws IOException {
        BinanceFuturesOrder binanceOrder = newOrder(
                order.getInstrument(),
                BinanceAdapters.convert(order.getType()),
                null,
                type,
                OrderType.LIMIT.equals(type) ? BinanceFuturesAdapters.timeInForceFromOrder(order).orElse(TimeInForce.IOC) : null,
                order.getOriginalAmount(),
                null,
                limitPrice,
                getClientOrderId(order),
                stopPrice,
                null,
                null,
                null,
                null,
                null
        );

        return Long.toString(binanceOrder.getOrderId());
    }

    @Override
    public boolean cancelOrder(CancelOrderParams orderParams) throws IOException {
        if (orderParams instanceof CancelOrderByCurrencyPair) {
            cancelAllOpenOrders(((CancelOrderByCurrencyPair) orderParams).getCurrencyPair());
            return true;
        } else {
            throw new ExchangeException("Binance Futures cancels must have pair and Client Order ID (ie. must implement CancelOrderByPairAndIdParams), or just pair to cancel all orders for that pair (ie. must implement CancelOrderByCurrencyPair)");
        }
    }

    @Override
    public Collection<Order> getOrder(OrderQueryParams... orderQueryParams) throws IOException {
        if (ArrayUtils.isEmpty(orderQueryParams)) return null;

        List<Order> orders = new ArrayList<>();

        for (OrderQueryParams orderQueryParam : orderQueryParams) {
            if (!(orderQueryParam instanceof OrderQueryParamCurrencyPair))
                throw new ExchangeException("Binance Futures order queries must have currency pair and order ID (ie. must implement OrderQueryParamCurrencyPair)");

            Long marketOrderId = null;
            try {
                marketOrderId = Long.valueOf(orderQueryParam.getOrderId());
            } catch (NumberFormatException ignored) {
            }

            BinanceFuturesOrder binanceOrderStatus = getOrderStatus(
                    ((OrderQueryParamCurrencyPair) orderQueryParam).getCurrencyPair(),
                    marketOrderId,
                    orderQueryParam.getOrderId()
            );

            orders.add(BinanceFuturesAdapters.adaptOrder(binanceOrderStatus));
        }

        return orders;
    }

    @Override
    public OpenOrders getOpenOrders() throws IOException {
        List<BinanceFuturesOrder> binanceFuturesOrders = getAllOpenOrders();

        return convertToOpenOrders(binanceFuturesOrders, x -> true);
    }

    @Override
    public OpenOrders getOpenOrders(OpenOrdersParams params) throws IOException {
        Predicate<Order> orderAcceptable = params != null ? params::accept : x -> true;

        if (params instanceof OpenOrdersParamCurrencyPair) {
            return convertToOpenOrders(getAllOpenOrders(((OpenOrdersParamCurrencyPair) params).getCurrencyPair()), orderAcceptable);
        } else {
            throw new ExchangeException("params must implement OpenOrdersParamCurrencyPair. If all orders are desired, use getOpenOrders()");
        }
    }

    private OpenOrders convertToOpenOrders(List<BinanceFuturesOrder> openBinanceFuturesOrders, Predicate<Order> interested) {
        List<LimitOrder> limitOrders = new ArrayList<>();
        List<Order> otherOrders = new ArrayList<>();
        openBinanceFuturesOrders.stream()
                .map(BinanceFuturesAdapters::adaptOrder)
                .filter(interested)
                .forEach(order -> {
                    if (order instanceof LimitOrder) {
                        limitOrders.add((LimitOrder) order);
                    } else {
                        otherOrders.add(order);
                    }
                });

        return new OpenOrders(limitOrders, otherOrders);
    }

    private String getClientOrderId(Order order) {
        for (Order.IOrderFlags orderFlag : order.getOrderFlags()) {
            if (orderFlag instanceof BinanceTradeService.BinanceOrderFlags) {
                return ((BinanceTradeService.BinanceOrderFlags) orderFlag).getClientId();
            }
        }

        return null;
    }

    @Override
    public OpenPositions getOpenPositions() throws IOException {
        List<BinancePosition> binancePositions = getAllOpenPositions();

        List<OpenPosition> openPositions = binancePositions.stream()
                .filter(pos -> BigDecimal.ZERO.compareTo(pos.getPositionAmt()) != 0) // Filter out zero positions
                .map(BinanceFuturesAdapters::adaptPosition)
                .collect(Collectors.toList());

        return new OpenPositions(openPositions);
    }
}
