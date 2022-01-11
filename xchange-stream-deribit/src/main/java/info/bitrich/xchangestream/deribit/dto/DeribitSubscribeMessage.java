package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitSubscribeMessage {
    private final DeribitSubscribeParams params;

    public DeribitSubscribeMessage(DeribitSubscribeParams params) {
        this.params = params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonrpc() {
        return "2.0";
    }

    @JsonProperty("method")
    public String getMethod() {
        return "public/subscribe";
    }

    @JsonProperty("params")
    public DeribitSubscribeParams getParams() {
        return params;
    }
}
