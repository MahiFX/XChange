package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitBaseMessage<T> {
    private final Long id;
    private final String method;
    private final T params;

    public DeribitBaseMessage(
            @JsonProperty("method") String method,
            @JsonProperty("params") T params) {
        this(null, method, params);
    }

    public DeribitBaseMessage(
            @JsonProperty("id") Long id,
            @JsonProperty("method") String method,
            @JsonProperty("params") T params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    @JsonProperty("jsonrpc")
    public String getJsonrpc() {
        return "2.0";
    }

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("method")
    public String getMethod() {
        return method;
    }

    @JsonProperty("params")
    public T getParams() {
        return params;
    }
}
