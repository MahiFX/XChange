package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitUnsubscribeMessage {
    private final DeribitSubscribeParams params;

    public DeribitUnsubscribeMessage(DeribitSubscribeParams params) {
        this.params = params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonrpc() {
        return "2.0";
    }

    @JsonProperty("method")
    public String getMethod() {
        return "public/unsubscribe";
    }

    @JsonProperty("params")
    public DeribitSubscribeParams getParams() {
        return params;
    }

}
