package org.knowm.xchange.binance.futures.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.OrderStatus;
import org.knowm.xchange.binance.dto.trade.TimeInForce;

import java.math.BigDecimal;

public class BinanceFuturesOrder {
    private final String clientOrderId;
    private final BigDecimal cumQty;
    private final BigDecimal cumQuote;
    private final BigDecimal executedQty;
    private final Long orderId;
    private final BigDecimal avgPrice;
    private final BigDecimal origQty;
    private final BigDecimal price;
    private final Boolean reduceOnly;
    private final OrderSide side;
    private final PositionSide positionSide;
    private final OrderStatus status;
    private final BigDecimal stopPrice;
    private final Boolean closePosition;
    private final String symbol;
    private final TimeInForce timeInForce;
    private final OrderType type;
    private final OrderType origType;
    private final BigDecimal activatePrice;
    private final BigDecimal priceRate;
    private final Long updateTime;
    private final WorkingType workingType;
    private final Boolean priceProtect;

    public BinanceFuturesOrder(
            @JsonProperty("clientOrderId") String clientOrderId,
            @JsonProperty("cumQty") BigDecimal cumQty,
            @JsonProperty("cumQuote") BigDecimal cumQuote,
            @JsonProperty("executedQty") BigDecimal executedQty,
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("avgPrice") BigDecimal avgPrice,
            @JsonProperty("origQty") BigDecimal origQty,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("reduceOnly") Boolean reduceOnly,
            @JsonProperty("side") OrderSide side,
            @JsonProperty("positionSide") PositionSide positionSide,
            @JsonProperty("status") OrderStatus status,
            @JsonProperty("stopPrice") BigDecimal stopPrice,
            @JsonProperty("closePosition") Boolean closePosition,
            @JsonProperty("symbol") String symbol,
            @JsonProperty("timeInForce") TimeInForce timeInForce,
            @JsonProperty("type") OrderType type,
            @JsonProperty("origType") OrderType origType,
            @JsonProperty("activatePrice") BigDecimal activatePrice,
            @JsonProperty("priceRate") BigDecimal priceRate,
            @JsonProperty("updateTime") Long updateTime,
            @JsonProperty("workingType") WorkingType workingType,
            @JsonProperty("priceProtect") Boolean priceProtect) {
        this.clientOrderId = clientOrderId;
        this.cumQty = cumQty;
        this.cumQuote = cumQuote;
        this.executedQty = executedQty;
        this.orderId = orderId;
        this.avgPrice = avgPrice;
        this.origQty = origQty;
        this.price = price;
        this.reduceOnly = reduceOnly;
        this.side = side;
        this.positionSide = positionSide;
        this.status = status;
        this.stopPrice = stopPrice;
        this.closePosition = closePosition;
        this.symbol = symbol;
        this.timeInForce = timeInForce;
        this.type = type;
        this.origType = origType;
        this.activatePrice = activatePrice;
        this.priceRate = priceRate;
        this.updateTime = updateTime;
        this.workingType = workingType;
        this.priceProtect = priceProtect;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public BigDecimal getCumQty() {
        return cumQty;
    }

    public BigDecimal getCumQuote() {
        return cumQuote;
    }

    public BigDecimal getExecutedQty() {
        return executedQty;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public BigDecimal getOrigQty() {
        return origQty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public OrderSide getSide() {
        return side;
    }

    public PositionSide getPositionSide() {
        return positionSide;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public Boolean getClosePosition() {
        return closePosition;
    }

    public String getSymbol() {
        return symbol;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public OrderType getType() {
        return type;
    }

    public OrderType getOrigType() {
        return origType;
    }

    public BigDecimal getActivatePrice() {
        return activatePrice;
    }

    public BigDecimal getPriceRate() {
        return priceRate;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public WorkingType getWorkingType() {
        return workingType;
    }

    public Boolean getPriceProtect() {
        return priceProtect;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + ((clientOrderId == null) ? "" : ", clientOrderId:" + clientOrderId)
                + ((cumQty == null) ? "" : ", cumQty:" + cumQty)
                + ((cumQuote == null) ? "" : ", cumQuote:" + cumQuote)
                + ((executedQty == null) ? "" : ", executedQty:" + executedQty)
                + ((orderId == null) ? "" : ", orderId:" + orderId)
                + ((avgPrice == null) ? "" : ", avgPrice:" + avgPrice)
                + ((origQty == null) ? "" : ", origQty:" + origQty)
                + ((price == null) ? "" : ", price:" + price)
                + ((reduceOnly == null) ? "" : ", reduceOnly:" + reduceOnly)
                + ((side == null) ? "" : ", side:" + side)
                + ((positionSide == null) ? "" : ", positionSide:" + positionSide)
                + ((status == null) ? "" : ", status:" + status)
                + ((stopPrice == null) ? "" : ", stopPrice:" + stopPrice)
                + ((closePosition == null) ? "" : ", closePosition:" + closePosition)
                + ((symbol == null) ? "" : ", symbol:" + symbol)
                + ((timeInForce == null) ? "" : ", timeInForce:" + timeInForce)
                + ((type == null) ? "" : ", type:" + type)
                + ((origType == null) ? "" : ", origType:" + origType)
                + ((activatePrice == null) ? "" : ", activatePrice:" + activatePrice)
                + ((priceRate == null) ? "" : ", priceRate:" + priceRate)
                + ((updateTime == null) ? "" : ", updateTime:" + updateTime)
                + ((workingType == null) ? "" : ", workingType:" + workingType)
                + ((priceProtect == null) ? "" : ", priceProtect:" + priceProtect)
                ;
    }
}
