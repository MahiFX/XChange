package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitOrderMessage {
    private final DeribitOrderParams params;
    private final String method;
    private final Long id;

    public DeribitOrderMessage(
            @JsonProperty("params") DeribitOrderParams params,
            @JsonProperty("method") String method,
            @JsonProperty("id") Long id) {
        this.params = params;
        this.method = method;
        this.id = id;
    }

    @JsonProperty("params")
    public DeribitOrderParams getParams() {
        return params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonrpc() {
        return "2.0";
    }

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }


    @JsonProperty("id")
    public Long getId() {
        return id;
    }
}
