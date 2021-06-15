package org.knowm.xchange.binance;

import org.knowm.xchange.binance.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.dto.OrderType;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.instrument.Instrument;

import java.util.Date;

public class BinanceFuturesAdapters {
    public static Order adaptOrder(BinanceFuturesOrder binanceFuturesOrder) {
        Order.Builder orderBuilder;
        Order.OrderType orderType = BinanceAdapters.convert(binanceFuturesOrder.getSide());
        Instrument instrument = BinanceAdapters.convert(binanceFuturesOrder.getSymbol());

        if (binanceFuturesOrder.getType().equals(OrderType.LIMIT)) {
            orderBuilder = new LimitOrder.Builder(orderType, instrument);
        } else if (binanceFuturesOrder.getType().equals(OrderType.MARKET)) {
            orderBuilder = new MarketOrder.Builder(orderType, instrument);
        } else {
            orderBuilder = new StopOrder.Builder(orderType, instrument);
        }

        orderBuilder
                .orderStatus(BinanceAdapters.adaptOrderStatus(binanceFuturesOrder.getStatus()))
                .originalAmount(binanceFuturesOrder.getOrigQty())
                .id(Long.toString(binanceFuturesOrder.getOrderId()))
                .timestamp(new Date(binanceFuturesOrder.getUpdateTime()))
                .cumulativeAmount(binanceFuturesOrder.getExecutedQty())
                .averagePrice(binanceFuturesOrder.getAvgPrice());

        if (binanceFuturesOrder.getClientOrderId() != null) {
            orderBuilder.flag(BinanceTradeService.BinanceOrderFlags.withClientId(binanceFuturesOrder.getClientOrderId()));
        }

        return orderBuilder.build();
    }
}
