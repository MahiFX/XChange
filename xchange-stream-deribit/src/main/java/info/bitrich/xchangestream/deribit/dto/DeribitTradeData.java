package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Trade;

import java.math.BigDecimal;
import java.util.Date;

public class DeribitTradeData {
    public final BigDecimal amount;
    public final String blockTradeId;
    public final String direction;
    public final BigDecimal indexPrice;
    public final String instrumentName;
    public final BigDecimal iv;
    public final String liquidation;
    public final BigDecimal markPrice;
    public final BigDecimal price;
    public final Integer tickDirection;
    public final Date timestamp;
    public final String tradeId;
    public final Integer tradeSeq;

    public DeribitTradeData(
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("block_trade_id") String blockTradeId,
            @JsonProperty("direction") String direction,
            @JsonProperty("index_price") BigDecimal indexPrice,
            @JsonProperty("instrument_name") String instrumentName,
            @JsonProperty("iv") BigDecimal iv,
            @JsonProperty("liquidation") String liquidation,
            @JsonProperty("mark_price") BigDecimal markPrice,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("tick_direction") Integer tickDirection,
            @JsonProperty("timestamp") Date timestamp,
            @JsonProperty("trade_id") String tradeId,
            @JsonProperty("trade_seq") Integer tradeSeq) {
        this.amount = amount;
        this.blockTradeId = blockTradeId;
        this.direction = direction;
        this.indexPrice = indexPrice;
        this.instrumentName = instrumentName;
        this.iv = iv;
        this.liquidation = liquidation;
        this.markPrice = markPrice;
        this.price = price;
        this.tickDirection = tickDirection;
        this.timestamp = timestamp;
        this.tradeId = tradeId;
        this.tradeSeq = tradeSeq;
    }

    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("block_trade_id")
    public String getBlockTradeId() {
        return blockTradeId;
    }

    @JsonProperty("direction")
    public String getDirection() {
        return direction;
    }

    @JsonProperty("index_price")
    public BigDecimal getIndexPrice() {
        return indexPrice;
    }

    @JsonProperty("instrument_name")
    public String getInstrumentName() {
        return instrumentName;
    }

    @JsonProperty("iv")
    public BigDecimal getIv() {
        return iv;
    }

    @JsonProperty("liquidation")
    public String getLiquidation() {
        return liquidation;
    }

    @JsonProperty("mark_price")
    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    @JsonProperty("price")
    public BigDecimal getPrice() {
        return price;
    }

    @JsonProperty("tick_direction")
    public Integer getTickDirection() {
        return tickDirection;
    }

    @JsonProperty("timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    @JsonProperty("trade_id")
    public String getTradeId() {
        return tradeId;
    }

    @JsonProperty("trade_seq")
    public Integer getTradeSeq() {
        return tradeSeq;
    }

    @JsonIgnore
    public Trade toTrade(CurrencyPair currencyPair) {
        return new Trade(
                // Side taker perspective
                "buy".equals(direction) ? Order.OrderType.ASK : Order.OrderType.BID,
                amount,
                currencyPair,
                price,
                timestamp,
                tradeId,
                null,
                null
        );
    }
}
