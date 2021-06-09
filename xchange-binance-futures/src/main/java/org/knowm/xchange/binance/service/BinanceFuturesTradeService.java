package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.binance.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.dto.OrderType;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.service.trade.TradeService;

import java.io.IOException;
import java.math.BigDecimal;

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

    private String getClientOrderId(Order order) {
        for (Order.IOrderFlags orderFlag : order.getOrderFlags()) {
            if (orderFlag instanceof BinanceTradeService.BinanceOrderFlags) {
                return ((BinanceTradeService.BinanceOrderFlags) orderFlag).getClientId();
            }
        }

        return null;
    }
}
