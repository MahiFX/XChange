package org.knowm.xchange.binance.futures;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.trade.BinanceOrderFlags;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.futures.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.futures.dto.BinancePosition;
import org.knowm.xchange.binance.futures.dto.OrderType;
import org.knowm.xchange.binance.futures.dto.PositionSide;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.StopOrder;
import org.knowm.xchange.instrument.Instrument;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public class BinanceFuturesAdapters {
    private BinanceFuturesAdapters() {
    }

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

    public static OpenPosition adaptPosition(BinancePosition binancePosition) {
        BigDecimal size = binancePosition.getPositionAmt();

        return new OpenPosition.Builder()
                .instrument(BinanceAdapters.convert(binancePosition.getSymbol()))
                .type(getType(binancePosition))
                .size(size)
                .price(binancePosition.getEntryPrice()).build();

    }

    private static OpenPosition.Type getType(BinancePosition binancePosition) {
        PositionSide positionSide = binancePosition.getPositionSide();

        if (PositionSide.BOTH.equals(positionSide)) {
            if (binancePosition.getPositionAmt().compareTo(BigDecimal.ZERO) < 0) {
                return OpenPosition.Type.SHORT;
            } else {
                return OpenPosition.Type.LONG;
            }
        } else {
            if (PositionSide.SHORT.equals(positionSide)) {
                return OpenPosition.Type.SHORT;
            } else if (PositionSide.LONG.equals(positionSide)) {
                return OpenPosition.Type.LONG;
            } else {
                throw new RuntimeException("Unexpected positionSide: " + positionSide);
            }
        }
    }

    public static Optional<TimeInForce> timeInForceFromOrder(Order order) {
        Optional<TimeInForce> timeInForce = BinanceAdapters.timeInForceFromOrder(order);

        return timeInForce.map(tif -> {
            if (TimeInForce.GTC.equals(tif)) {
                if (order.hasFlag(BinanceOrderFlags.LIMIT_MAKER)) {
                    return TimeInForce.GTX;
                }
            }

            return tif;
        });
    }
}
