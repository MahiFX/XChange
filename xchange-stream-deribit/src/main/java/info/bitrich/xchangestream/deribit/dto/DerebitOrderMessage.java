package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DerebitOrderMessage {
    private final DerebitOrderParams params;
    private final String method;

    public DerebitOrderMessage(DerebitOrderParams params, String method) {
        this.params = params;
        this.method = method;
    }

    @JsonProperty("params")
    public DerebitOrderParams getParams() {
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
}
