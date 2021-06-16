package org.knowm.xchange.binance;

import org.junit.Test;
import org.knowm.xchange.binance.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.dto.OrderType;
import org.knowm.xchange.binance.dto.PositionSide;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.OrderStatus;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class BinanceFuturesAdaptersTest {
    @Test
    public void testOrderAdapt() {
        BinanceFuturesOrder binanceFuturesOrder = new BinanceFuturesOrder(
                "abc123",
                new BigDecimal("50"),
                new BigDecimal("5"),
                new BigDecimal("50"),
                12345L,
                new BigDecimal("39500.50"),
                new BigDecimal("100"),
                new BigDecimal("40000.0"),
                false,
                OrderSide.BUY,
                PositionSide.LONG,
                OrderStatus.PARTIALLY_FILLED,
                null,
                false,
                "BTCUSDT",
                TimeInForce.GTC,
                OrderType.LIMIT,
                OrderType.LIMIT,
                null,
                null,
                1622696224000L,
                null,
                false
        );

        Order converted = BinanceFuturesAdapters.adaptOrder(binanceFuturesOrder);

        assertThat(converted).isInstanceOf(LimitOrder.class);
        assertThat(converted.getStatus()).isEqualTo(Order.OrderStatus.PARTIALLY_FILLED);
        assertThat(converted.getOriginalAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(converted.getId()).isEqualTo("12345");
        assertThat(converted.getTimestamp()).isEqualTo(new Date(1622696224000L));
        assertThat(converted.getCumulativeAmount()).isEqualTo(new BigDecimal("50"));
        assertThat(converted.getAveragePrice()).isEqualTo(new BigDecimal("39500.50"));

        assertThat(converted.getOrderFlags()).isNotEmpty();
        assertThat(converted.getOrderFlags()).contains(BinanceTradeService.BinanceOrderFlags.withClientId("abc123"));
    }
}