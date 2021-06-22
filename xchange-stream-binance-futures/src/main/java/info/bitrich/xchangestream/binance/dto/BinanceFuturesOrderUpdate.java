package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class BinanceFuturesOrderUpdate {
    private final String eventType;
    private final String eventTime;
    private final OrderDetails orderDetails;

    public BinanceFuturesOrderUpdate(
            @JsonProperty("e") String eventType,
            @JsonProperty("E") String eventTime,
            @JsonProperty("o") OrderDetails orderDetails) {
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.orderDetails = orderDetails;
    }

    @JsonIgnore
    public ExecutionReportBinanceUserTransaction toBinanceExecutionReport() {
        return new ExecutionReportBinanceUserTransaction(
                eventType,
                eventTime,
                orderDetails.symbol,
                orderDetails.clientOrderId,
                orderDetails.side,
                orderDetails.orderType,
                orderDetails.timeInForce,
                orderDetails.quantity,
                orderDetails.price,
                orderDetails.stopPrice,
                null,
                orderDetails.executionType,
                orderDetails.status,
                null,
                orderDetails.orderId,
                orderDetails.lastExecutedQuantity,
                orderDetails.cumulativeFilledQuantity,
                orderDetails.lastExecutedPrice,
                orderDetails.commission,
                orderDetails.commissionAsset,
                orderDetails.timestamp,
                orderDetails.tradeId,
                true,
                orderDetails.isMaker,
                orderDetails.cumulativeFilledQuantity.multiply(orderDetails.averagePrice)
        );
    }

    private static class OrderDetails {
        private final String symbol;
        private final String clientOrderId;
        private final String side;
        private final String orderType;
        private final String timeInForce;
        private final BigDecimal quantity;
        private final BigDecimal price;
        private final BigDecimal stopPrice;
        private final String executionType;
        private final String status;
        private final long orderId;
        private final BigDecimal lastExecutedQuantity;
        private final BigDecimal cumulativeFilledQuantity;
        private final BigDecimal lastExecutedPrice;
        private final BigDecimal commission;
        private final String commissionAsset;
        private final long timestamp;
        private final long tradeId;
        private final boolean isMaker;
        private final BigDecimal averagePrice;

        public OrderDetails(
                @JsonProperty("s") String symbol,
                @JsonProperty("c") String clientOrderId,
                @JsonProperty("S") String side,
                @JsonProperty("o") String orderType,
                @JsonProperty("f") String timeInForce,
                @JsonProperty("q") BigDecimal quantity,
                @JsonProperty("p") BigDecimal price,
                @JsonProperty("sp") BigDecimal stopPrice,
                @JsonProperty("x") String executionType,
                @JsonProperty("X") String status,
                @JsonProperty("i") long orderId,
                @JsonProperty("l") BigDecimal lastExecutedQuantity,
                @JsonProperty("z") BigDecimal cumulativeFilledQuantity,
                @JsonProperty("L") BigDecimal lastExecutedPrice,
                @JsonProperty("n") BigDecimal commission,
                @JsonProperty("N") String commissionAsset,
                @JsonProperty("T") long timestamp,
                @JsonProperty("t") long tradeId,
                @JsonProperty("m") boolean isMaker,
                @JsonProperty("ap") BigDecimal averagePrice) {
            this.symbol = symbol;
            this.clientOrderId = clientOrderId;
            this.side = side;
            this.orderType = orderType;
            this.timeInForce = timeInForce;
            this.quantity = quantity;
            this.price = price;
            this.stopPrice = stopPrice;
            this.executionType = executionType;
            this.status = status;
            this.orderId = orderId;
            this.lastExecutedQuantity = lastExecutedQuantity;
            this.cumulativeFilledQuantity = cumulativeFilledQuantity;
            this.lastExecutedPrice = lastExecutedPrice;
            this.commission = commission;
            this.commissionAsset = commissionAsset;
            this.timestamp = timestamp;
            this.tradeId = tradeId;
            this.isMaker = isMaker;
            this.averagePrice = averagePrice;
        }
    }
}
