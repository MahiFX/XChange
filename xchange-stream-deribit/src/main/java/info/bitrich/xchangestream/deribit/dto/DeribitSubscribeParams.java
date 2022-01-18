package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitSubscribeParams {
    private final String[] channels;
    private final String label;
    private final String accessToken;

    public DeribitSubscribeParams(String channel) {
        this.channels = new String[]{channel};
        this.label = null;
        this.accessToken = null;
    }

    public DeribitSubscribeParams(String channel, String label, String accessToken) {
        this.channels = new String[]{channel};
        this.label = label;
        this.accessToken = accessToken;
    }

    @JsonProperty("channels")
    public String[] getChannels() {
        return channels;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }
}
