package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.OpenPosition;

import java.math.BigDecimal;


public class DeribitPosition {
    private final BigDecimal averagePrice;
    private final BigDecimal delta;
    private final String direction;
    private final BigDecimal estimatedLiquidationPrice;
    private final BigDecimal floatingPointLoss;
    private final BigDecimal indexPrice;
    private final BigDecimal initialMargin;
    private final String instrumentName;
    private final String kind;
    private final BigDecimal maintenanceMargin;
    private final BigDecimal markPrice;
    private final BigDecimal openOrdersMargin;
    private final BigDecimal realizedFunding;
    private final BigDecimal realizedProfitLoss;
    private final BigDecimal settlementPrice;
    private final BigDecimal size;
    private final BigDecimal sizeCurrency;
    private final BigDecimal totalProfitLoss;

    public DeribitPosition(
            @JsonProperty("average_price") BigDecimal averagePrice,
            @JsonProperty("delta") BigDecimal delta,
            @JsonProperty("direction") String direction,
            @JsonProperty("estimated_liquidation_price") BigDecimal estimatedLiquidationPrice,
            @JsonProperty("floating_point_loss") BigDecimal floatingPointLoss,
            @JsonProperty("index_price") BigDecimal indexPrice,
            @JsonProperty("initial_margin") BigDecimal initialMargin,
            @JsonProperty("instrument_name") String instrumentName,
            @JsonProperty("kind") String kind,
            @JsonProperty("maintenance_margin") BigDecimal maintenanceMargin,
            @JsonProperty("mark_price") BigDecimal markPrice,
            @JsonProperty("open_orders_margin") BigDecimal openOrdersMargin,
            @JsonProperty("realized_funding") BigDecimal realizedFunding,
            @JsonProperty("realized_profit_loss") BigDecimal realizedProfitLoss,
            @JsonProperty("settlement_price") BigDecimal settlementPrice,
            @JsonProperty("size") BigDecimal size,
            @JsonProperty("size_currency") BigDecimal sizeCurrency,
            @JsonProperty("total_profit_loss") BigDecimal totalProfitLoss) {
        this.averagePrice = averagePrice;
        this.delta = delta;
        this.direction = direction;
        this.estimatedLiquidationPrice = estimatedLiquidationPrice;
        this.floatingPointLoss = floatingPointLoss;
        this.indexPrice = indexPrice;
        this.initialMargin = initialMargin;
        this.instrumentName = instrumentName;
        this.kind = kind;
        this.maintenanceMargin = maintenanceMargin;
        this.markPrice = markPrice;
        this.openOrdersMargin = openOrdersMargin;
        this.realizedFunding = realizedFunding;
        this.realizedProfitLoss = realizedProfitLoss;
        this.settlementPrice = settlementPrice;
        this.size = size;
        this.sizeCurrency = sizeCurrency;
        this.totalProfitLoss = totalProfitLoss;
    }

    @JsonProperty("average_price")
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    @JsonProperty("delta")
    public BigDecimal getDelta() {
        return delta;
    }

    @JsonProperty("direction")
    public String getDirection() {
        return direction;
    }

    @JsonProperty("estimated_liquidation_price")
    public BigDecimal getEstimatedLiquidationPrice() {
        return estimatedLiquidationPrice;
    }

    @JsonProperty("floating_point_loss")
    public BigDecimal getFloatingPointLoss() {
        return floatingPointLoss;
    }

    @JsonProperty("index_price")
    public BigDecimal getIndexPrice() {
        return indexPrice;
    }

    @JsonProperty("initial_margin")
    public BigDecimal getInitialMargin() {
        return initialMargin;
    }

    @JsonProperty("instrument_name")
    public String getInstrumentName() {
        return instrumentName;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("maintenance_margin")
    public BigDecimal getMaintenanceMargin() {
        return maintenanceMargin;
    }

    @JsonProperty("mark_price")
    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    @JsonProperty("open_orders_margin")
    public BigDecimal getOpenOrdersMargin() {
        return openOrdersMargin;
    }

    @JsonProperty("realized_funding")
    public BigDecimal getRealizedFunding() {
        return realizedFunding;
    }

    @JsonProperty("realized_profit_loss")
    public BigDecimal getRealizedProfitLoss() {
        return realizedProfitLoss;
    }

    @JsonProperty("settlement_price")
    public BigDecimal getSettlementPrice() {
        return settlementPrice;
    }

    @JsonProperty("size")
    public BigDecimal getSize() {
        return size;
    }

    @JsonProperty("size_currency")
    public BigDecimal getSizeCurrency() {
        return sizeCurrency;
    }

    @JsonProperty("total_profit_loss")
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }

    @JsonIgnore
    public OpenPosition toOpenPosition() {
        return new OpenPosition.Builder()
                .instrument(new CurrencyPair(instrumentName))
                .type("buy".equals(direction) ? OpenPosition.Type.LONG : OpenPosition.Type.SHORT)
                .size(size)
                .price(averagePrice).build();
    }
}
