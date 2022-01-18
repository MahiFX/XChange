package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitAuthMessage {
    private final DeribitAuthParams params;

    public DeribitAuthMessage(DeribitAuthParams params) {
        this.params = params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonrpc() {
        return "2.0";
    }

    @JsonProperty("method")
    public String getMethod() {
        return "public/auth";
    }

    @JsonProperty("params")
    public DeribitAuthParams getParams() {
        return params;
    }
}
