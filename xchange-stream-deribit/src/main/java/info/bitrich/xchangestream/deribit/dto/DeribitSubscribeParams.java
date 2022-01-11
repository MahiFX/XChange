package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitSubscribeParams {
    private final String[] channels;
    private final String label;

    public DeribitSubscribeParams(String channel) {
        this.channels = new String[]{channel};
        this.label = null;
    }

    public DeribitSubscribeParams(String channel, String label) {
        this.channels = new String[]{channel};
        this.label = label;
    }

    @JsonProperty("channels")
    public String[] getChannels() {
        return channels;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }
}
