package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitCancelOrderMessage {
    private final long id;
    private final DeribitCancelOrderParams params;

    public DeribitCancelOrderMessage(long messageId, String orderId) {
        this.id = messageId;
        this.params = new DeribitCancelOrderParams(orderId);
    }

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("params")
    public DeribitCancelOrderParams getParams() {
        return params;
    }

    @JsonProperty("method")
    public String getMethod() {
        return "private/cancel";
    }

    @JsonProperty("jsonrpc")
    public String getJsonRpc() {
        return "2.0";
    }

    private static class DeribitCancelOrderParams {
        private final String orderId;

        public DeribitCancelOrderParams(@JsonProperty("order_id") String orderId) {
            this.orderId = orderId;
        }

        @JsonProperty("order_id")
        public String getOrderId() {
            return orderId;
        }
    }
}
