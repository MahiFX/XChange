package org.knowm.xchange.binance.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.OrderStatus;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.futures.dto.BinanceFuturesOrder;
import org.knowm.xchange.binance.futures.dto.OrderType;
import org.knowm.xchange.binance.futures.dto.PositionSide;
import org.knowm.xchange.binance.futures.dto.WorkingType;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class BinanceFuturesOrderTest {
    @Test
    public void testUnmarshall() throws IOException {
        InputStream is = BinanceFuturesOrderTest.class.getResourceAsStream(
                "/org/knowm/xchange/binance/dto/OrderStatus.json");

        ObjectMapper mapper = new ObjectMapper();
        BinanceFuturesOrder order = mapper.readValue(is, BinanceFuturesOrder.class);

        assertThat(order.getClientOrderId()).isEqualTo("testOrder");
        assertThat(order.getCumQty()).isEqualTo(new BigDecimal("0"));
        assertThat(order.getCumQuote()).isEqualTo(new BigDecimal("0"));
        assertThat(order.getExecutedQty()).isEqualTo(new BigDecimal("0"));
        assertThat(order.getOrderId()).isEqualTo(22542179);
        assertThat(order.getAvgPrice()).isEqualTo(new BigDecimal("0.00000"));
        assertThat(order.getOrigQty()).isEqualTo(new BigDecimal("10"));
        assertThat(order.getPrice()).isEqualTo(new BigDecimal("0"));
        assertThat(order.getReduceOnly()).isEqualTo(false);
        assertThat(order.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(order.getPositionSide()).isEqualTo(PositionSide.SHORT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getStopPrice()).isEqualTo(new BigDecimal("9300"));
        assertThat(order.getClosePosition()).isEqualTo(false);
        assertThat(order.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(order.getTimeInForce()).isEqualTo(TimeInForce.GTC);
        assertThat(order.getType()).isEqualTo(OrderType.TRAILING_STOP_MARKET);
        assertThat(order.getOrigType()).isEqualTo(OrderType.TRAILING_STOP_MARKET);
        assertThat(order.getActivatePrice()).isEqualTo(new BigDecimal("9020"));
        assertThat(order.getPriceRate()).isEqualTo(new BigDecimal("0.3"));
        assertThat(order.getUpdateTime()).isEqualTo(1566818724722L);
        assertThat(order.getWorkingType()).isEqualTo(WorkingType.CONTRACT_PRICE);
        assertThat(order.getPriceProtect()).isEqualTo(false);
    }
}
