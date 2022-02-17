package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitCancelOrderParams {
    private final String orderId;

    public DeribitCancelOrderParams(@JsonProperty("order_id") String orderId) {
        this.orderId = orderId;
    }

    @JsonProperty("order_id")
    public String getOrderId() {
        return orderId;
    }
}
