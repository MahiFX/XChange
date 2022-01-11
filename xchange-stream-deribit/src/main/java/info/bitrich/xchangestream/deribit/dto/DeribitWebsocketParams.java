package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitWebsocketParams {
    private final String channel;
    private final String label;
    private final Object data;

    public DeribitWebsocketParams(
            @JsonProperty("channel") String channel,
            @JsonProperty("label") String label,
            @JsonProperty("data") Object data) {
        this.channel = channel;
        this.label = label;
        this.data = data;
    }

    @JsonProperty("channel")
    public String getChannel() {
        return channel;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("data")
    public Object getData() {
        return data;
    }
}
