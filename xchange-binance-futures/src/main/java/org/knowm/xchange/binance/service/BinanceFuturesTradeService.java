package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.BinanceFuturesAdapters;
import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.binance.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.dto.OrderType;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.CancelOrderByCurrencyPair;
import org.knowm.xchange.service.trade.params.CancelOrderByPairAndIdParams;
import org.knowm.xchange.service.trade.params.CancelOrderParams;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParamCurrencyPair;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
                order.getCurrencyPair(),
                BinanceAdapters.convert(order.getType()),
                null,
                type,
                BinanceAdapters.timeInForceFromOrder(order).orElse(TimeInForce.IOC),
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
        if (orderParams instanceof CancelOrderByPairAndIdParams) {
            CancelOrderByPairAndIdParams pairAndIdParams = (CancelOrderByPairAndIdParams) orderParams;
            BinanceFuturesOrder cancelResult = cancelOrder(
                    pairAndIdParams.getCurrencyPair(),
                    null,
                    pairAndIdParams.getOrderId());

            return cancelResult != null;
        } else if (orderParams instanceof CancelOrderByCurrencyPair) {
            cancelAllOpenOrders(((CancelOrderByCurrencyPair) orderParams).getCurrencyPair());
            return true;
        } else {
            throw new ExchangeException("Binance Futures cancels must have pair and Client Order ID (ie. must implement CancelOrderByPairAndIdParams), or just pair to cancel all orders for that pair (ie. must implement CancelOrderByCurrencyPair)");
        }
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
}
