package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitWebsocketResponse {
    private final String jsonRpc;
    private final String method;
    private final DeribitWebsocketParams params;

    public DeribitWebsocketResponse(
            @JsonProperty("jsonrpc") String jsonRpc,
            @JsonProperty("method") String method,
            @JsonProperty("params") DeribitWebsocketParams params) {
        this.jsonRpc = jsonRpc;
        this.method = method;
        this.params = params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonRpc() {
        return jsonRpc;
    }

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    @JsonProperty("params")
    public DeribitWebsocketParams getParams() {
        return params;
    }
}
