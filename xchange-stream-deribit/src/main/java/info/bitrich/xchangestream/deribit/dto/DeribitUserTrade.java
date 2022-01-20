package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.deribit.DeribitStreamingUtil;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.UserTrade;

import java.math.BigDecimal;
import java.util.Date;

public class DeribitUserTrade {
    public static DeribitUserTrade EMPTY = new DeribitUserTrade(null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    private final BigDecimal amount;
    private final String blockTradeId;
    private final String direction;
    private final BigDecimal fee;
    private final String feeCurrency;
    private final BigDecimal indexPrice;
    private final String instrumentName;
    private final BigDecimal iv;
    private final String label;
    private final String liquidation;
    private final String liquidity;
    private final BigDecimal markPrice;
    private final String matchingId;
    private final String orderId;
    private final String orderType;
    private final String postOnly;
    private final BigDecimal price;
    private final BigDecimal profitLoss;
    private final String reduceOnly;
    private final Boolean selfTrade;
    private final String state;
    private final Integer tickDirection;
    private final Date timestamp;
    private final String tradeId;
    private final Integer tradeSeq;
    private final BigDecimal underlyingPrice;

    public DeribitUserTrade(@JsonProperty("amount") BigDecimal amount, @JsonProperty("block_trade_id") String blockTradeId,
                            @JsonProperty("direction") String direction, @JsonProperty("fee") BigDecimal fee,
                            @JsonProperty("fee_currency") String feeCurrency, @JsonProperty("index_price") BigDecimal indexPrice,
                            @JsonProperty("instrument_name") String instrumentName, @JsonProperty("iv") BigDecimal iv,
                            @JsonProperty("label") String label, @JsonProperty("liquidation") String liquidation,
                            @JsonProperty("liquidity") String liquidity, @JsonProperty("mark_price") BigDecimal markPrice,
                            @JsonProperty("matching_id") String matchingId, @JsonProperty("order_id") String orderId,
                            @JsonProperty("order_type") String orderType, @JsonProperty("post_only") String postOnly,
                            @JsonProperty("price") BigDecimal price, @JsonProperty("profit_loss") BigDecimal profitLoss,
                            @JsonProperty("reduce_only") String reduceOnly, @JsonProperty("self_trade") Boolean selfTrade,
                            @JsonProperty("state") String state, @JsonProperty("tick_direction") Integer tickDirection,
                            @JsonProperty("timestamp") Date timestamp, @JsonProperty("trade_id") String tradeId,
                            @JsonProperty("trade_seq") Integer tradeSeq, @JsonProperty("underlying_price") BigDecimal underlyingPrice) {
        this.amount = amount;
        this.blockTradeId = blockTradeId;
        this.direction = direction;
        this.fee = fee;
        this.feeCurrency = feeCurrency;
        this.indexPrice = indexPrice;
        this.instrumentName = instrumentName;
        this.iv = iv;
        this.label = label;
        this.liquidation = liquidation;
        this.liquidity = liquidity;
        this.markPrice = markPrice;
        this.matchingId = matchingId;
        this.orderId = orderId;
        this.orderType = orderType;
        this.postOnly = postOnly;
        this.price = price;
        this.profitLoss = profitLoss;
        this.reduceOnly = reduceOnly;
        this.selfTrade = selfTrade;
        this.state = state;
        this.tickDirection = tickDirection;
        this.timestamp = timestamp;
        this.tradeId = tradeId;
        this.tradeSeq = tradeSeq;
        this.underlyingPrice = underlyingPrice;
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

    @JsonProperty("fee")
    public BigDecimal getFee() {
        return fee;
    }

    @JsonProperty("fee_currency")
    public String getFeeCurrency() {
        return feeCurrency;
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

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("liquidation")
    public String getLiquidation() {
        return liquidation;
    }

    @JsonProperty("liquidity")
    public String getLiquidity() {
        return liquidity;
    }
    
    @JsonProperty("mark_price")
    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    @JsonProperty("matching_id")
    public String getMatchingId() {
        return matchingId;
    }

    @JsonProperty("order_id")
    public String getOrderId() {
        return orderId;
    }

    @JsonProperty("order_type")
    public String getOrderType() {
        return orderType;
    }

    @JsonProperty("post_only")
    public String getPostOnly() {
        return postOnly;
    }

    @JsonProperty("price")
    public BigDecimal getPrice() {
        return price;
    }

    @JsonProperty("profit_loss")
    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    @JsonProperty("reduce_only")
    public String getReduceOnly() {
        return reduceOnly;
    }

    @JsonProperty("self_trade")
    public Boolean getSelfTrade() {
        return selfTrade;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
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

    @JsonProperty("underlying_price")
    public BigDecimal getUnderlyingPrice() {
        return underlyingPrice;
    }

    @JsonIgnore
    public UserTrade toUserTrade() {
        return new UserTrade(DeribitStreamingUtil.convertDirectionToType(getDirection()), getAmount(),
                new CurrencyPair(getInstrumentName()), getPrice(), getTimestamp(), getTradeId(), getOrderId(),
                getFee(), new Currency(getFeeCurrency()), getLabel());
    }
}
