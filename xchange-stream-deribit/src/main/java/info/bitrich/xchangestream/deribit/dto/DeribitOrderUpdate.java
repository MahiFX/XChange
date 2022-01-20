package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.deribit.DeribitStreamingUtil;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;

import java.math.BigDecimal;
import java.util.Date;

public class DeribitOrderUpdate {
    public static DeribitOrderUpdate EMPTY = new DeribitOrderUpdate(false, null, null, false, false, null, false, null, null, false, false, null, null, null, false, null, false, false, null, null, null, false, null, null, null, null, null, null, null, false, null, null, null, null, null);

    private final boolean mmpCancelled;
    private final OrderState orderState;
    private final BigDecimal maxShow;
    private final boolean rejectPostOnly;
    private final boolean api;
    private final BigDecimal amount;
    private final boolean web;
    private final String instrumentName;
    private final String advanced;
    private final boolean triggered;
    private final boolean blockTrade;
    private final String originalOrderType;
    private final BigDecimal price;
    private final TimeInForce timeInForce;
    private final boolean autoReplaced;
    private final Date lastUpdateTimestamp;
    private final boolean postOnly;
    private final boolean replaced;
    private final BigDecimal filledAmount;
    private final BigDecimal averagePrice;
    private final String orderId;
    private final boolean reduceOnly;
    private final BigDecimal commission;
    private final String appName;
    private final String label;
    private final String triggerOrderId;
    private final BigDecimal triggerPrice;
    private final Date creationTimestamp;
    private final String direction;
    private final boolean isLiquidation;
    private final String orderType;
    private final BigDecimal usd;
    private final BigDecimal profitLoss;
    private final BigDecimal implv;
    private final String trigger;

    public DeribitOrderUpdate(
            @JsonProperty("mmp_cancelled") boolean mmpCancelled,
            @JsonProperty("order_state") OrderState orderState,
            @JsonProperty("max_show") BigDecimal maxShow,
            @JsonProperty("reject_post_only") boolean rejectPostOnly,
            @JsonProperty("api") boolean api,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("web") boolean web,
            @JsonProperty("instrument_name") String instrumentName,
            @JsonProperty("advanced") String advanced,
            @JsonProperty("triggered") boolean triggered,
            @JsonProperty("block_trade") boolean blockTrade,
            @JsonProperty("original_order_type") String originalOrderType,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("time_in_force") TimeInForce timeInForce,
            @JsonProperty("auto_replaced") boolean autoReplaced,
            @JsonProperty("last_update_timestamp") Date lastUpdateTimestamp,
            @JsonProperty("post_only") boolean postOnly,
            @JsonProperty("replaced") boolean replaced,
            @JsonProperty("filled_amount") BigDecimal filledAmount,
            @JsonProperty("average_price") BigDecimal averagePrice,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("reduce_only") boolean reduceOnly,
            @JsonProperty("commission") BigDecimal commission,
            @JsonProperty("app_name") String appName,
            @JsonProperty("label") String label,
            @JsonProperty("trigger_order_id") String triggerOrderId,
            @JsonProperty("trigger_price") BigDecimal triggerPrice,
            @JsonProperty("creation_timestamp") Date creationTimestamp,
            @JsonProperty("direction") String direction,
            @JsonProperty("liquidation") boolean isLiquidation,
            @JsonProperty("order_type") String orderType,
            @JsonProperty("usd") BigDecimal usd,
            @JsonProperty("profit_loss") BigDecimal profitLoss,
            @JsonProperty("implv") BigDecimal implv,
            @JsonProperty("trigger") String trigger) {
        this.mmpCancelled = mmpCancelled;
        this.orderState = orderState;
        this.maxShow = maxShow;
        this.rejectPostOnly = rejectPostOnly;
        this.api = api;
        this.amount = amount;
        this.web = web;
        this.instrumentName = instrumentName;
        this.advanced = advanced;
        this.triggered = triggered;
        this.blockTrade = blockTrade;
        this.originalOrderType = originalOrderType;
        this.price = price;
        this.timeInForce = timeInForce;
        this.autoReplaced = autoReplaced;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
        this.postOnly = postOnly;
        this.replaced = replaced;
        this.filledAmount = filledAmount;
        this.averagePrice = averagePrice;
        this.orderId = orderId;
        this.reduceOnly = reduceOnly;
        this.commission = commission;
        this.appName = appName;
        this.label = label;
        this.triggerOrderId = triggerOrderId;
        this.triggerPrice = triggerPrice;
        this.creationTimestamp = creationTimestamp;
        this.direction = direction;
        this.isLiquidation = isLiquidation;
        this.orderType = orderType;
        this.usd = usd;
        this.profitLoss = profitLoss;
        this.implv = implv;
        this.trigger = trigger;
    }

    @JsonProperty("mmp_cancelled")
    public boolean isMmpCancelled() {
        return mmpCancelled;
    }

    @JsonProperty("order_state")
    public OrderState getOrderState() {
        return orderState;
    }

    @JsonProperty("max_show")
    public BigDecimal getMaxShow() {
        return maxShow;
    }

    @JsonProperty("reject_post_only")
    public boolean isRejectPostOnly() {
        return rejectPostOnly;
    }

    @JsonProperty("api")
    public boolean isApi() {
        return api;
    }

    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("web")
    public boolean isWeb() {
        return web;
    }

    @JsonProperty("instrument_name")
    public String getInstrumentName() {
        return instrumentName;
    }

    @JsonProperty("advanced")
    public String getAdvanced() {
        return advanced;
    }

    @JsonProperty("triggered")
    public boolean isTriggered() {
        return triggered;
    }

    @JsonProperty("block_trade")
    public boolean isBlockTrade() {
        return blockTrade;
    }

    @JsonProperty("original_order_type")
    public String getOriginalOrderType() {
        return originalOrderType;
    }

    @JsonProperty("price")
    public BigDecimal getPrice() {
        return price;
    }

    @JsonProperty("time_in_force")
    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    @JsonProperty("auto_replaced")
    public boolean isAutoReplaced() {
        return autoReplaced;
    }

    @JsonProperty("last_update_timestamp")
    public Date getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    @JsonProperty("post_only")
    public boolean isPostOnly() {
        return postOnly;
    }

    @JsonProperty("replaced")
    public boolean isReplaced() {
        return replaced;
    }

    @JsonProperty("filled_amount")
    public BigDecimal getFilledAmount() {
        return filledAmount;
    }

    @JsonProperty("average_price")
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    @JsonProperty("order_id")
    public String getOrderId() {
        return orderId;
    }

    @JsonProperty("reduce_only")
    public boolean isReduceOnly() {
        return reduceOnly;
    }

    @JsonProperty("commission")
    public BigDecimal getCommission() {
        return commission;
    }

    @JsonProperty("app_name")
    public String getAppName() {
        return appName;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("trigger_order_id")
    public String getTriggerOrderId() {
        return triggerOrderId;
    }

    @JsonProperty("trigger_price")
    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    @JsonProperty("creation_timestamp")
    public Date getCreationTimestamp() {
        return creationTimestamp;
    }

    @JsonProperty("direction")
    public String getDirection() {
        return direction;
    }

    @JsonProperty("liquidation")
    public boolean isLiquidation() {
        return isLiquidation;
    }

    @JsonProperty("order_type")
    public String getOrderType() {
        return orderType;
    }

    @JsonProperty("usd")
    public BigDecimal getUsd() {
        return usd;
    }

    @JsonProperty("profit_loss")
    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    @JsonProperty("implv")
    public BigDecimal getImplv() {
        return implv;
    }

    @JsonProperty("trigger")
    public String getTrigger() {
        return trigger;
    }

    @JsonIgnore
    public Order toOrder() {
        Order.Builder orderBuilder;

        CurrencyPair instrument = new CurrencyPair(instrumentName);
        Order.OrderType type = DeribitStreamingUtil.getType(direction);
        if (orderType.contains("market")) {
            orderBuilder = new MarketOrder.Builder(type, instrument);
        } else {
            orderBuilder = new LimitOrder.Builder(type, instrument)
                    .limitPrice(price);
        }

        return orderBuilder
                .originalAmount(amount)
                .instrument(instrument)
                .id(orderId)
                .timestamp(lastUpdateTimestamp)
                .averagePrice(averagePrice)
                .cumulativeAmount(filledAmount)
                .orderStatus(orderStateToStatus())
                .userReference(label)
                .build();
    }

    private Order.OrderStatus orderStateToStatus() {
        if (OrderState.FILLED.equals(orderState)) {
            if (filledAmount.equals(amount)) {
                return Order.OrderStatus.FILLED;
            } else {
                return Order.OrderStatus.PARTIALLY_FILLED;
            }
        } else if (OrderState.CANCELLED.equals(orderState)) {
            return Order.OrderStatus.CANCELED;
        } else if (OrderState.REJECTED.equals(orderState)) {
            return Order.OrderStatus.REJECTED;
        } else if (OrderState.OPEN.equals(orderState)) {
            return Order.OrderStatus.OPEN;
        } else {
            throw new RuntimeException("Could not map XChange OrderStatus from orderState: " + orderState);
        }
    }

    public enum OrderState {
        OPEN("open"),
        FILLED("filled"),
        REJECTED("rejected"),
        CANCELLED("cancelled"),
        UNTRIGGERED("untriggered");

        private final String state;

        OrderState(String state) {
            this.state = state;
        }
    }

    public enum TimeInForce {
        GTC("good_til_cancelled"),
        GTD("good_til_day"),
        FOK("fill_or_kill"),
        IOC("immediate_or_cancel");

        private final String name;

        TimeInForce(String name) {
            this.name = name;
        }
    }
}
