package org.knowm.xchange.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class BinancePosition {
    private final String symbol;
    private final BigDecimal entryPrice;
    private final BigDecimal markPrice;
    private final BigDecimal positionAmt;
    private final PositionSide positionSide;

    public BinancePosition(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("entryPrice") BigDecimal entryPrice,
            @JsonProperty("markPrice") BigDecimal markPrice,
            @JsonProperty("positionAmt") BigDecimal positionAmt,
            @JsonProperty("positionSide") PositionSide positionSide) {
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.markPrice = markPrice;
        this.positionAmt = positionAmt;
        this.positionSide = positionSide;
    }

    @JsonProperty("symbol")
    public String getSymbol() {
        return symbol;
    }

    @JsonProperty("entryPrice")
    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    @JsonProperty("markPrice")
    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    @JsonProperty("positionAmt")
    public BigDecimal getPositionAmt() {
        return positionAmt;
    }

    @JsonProperty("positionSide")
    public PositionSide getPositionSide() {
        return positionSide;
    }
}
