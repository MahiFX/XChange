package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class CryptoFacilitiesFillsMessage {
    private final String feed;
    private final String username;
    private final String account;
    private final CryptoFacilitiesFill[] fills;

    public CryptoFacilitiesFillsMessage(
            @JsonProperty("feed") String feed,
            @JsonProperty("account") String account,
            @JsonProperty("username") String username,
            @JsonProperty("fills") CryptoFacilitiesFill[] fills) {
        this.feed = feed;
        this.account = account;
        this.username = username;
        this.fills = fills;
    }

    public String getFeed() {
        return feed;
    }

    public String getUsername() {
        return username;
    }

    public String getAccount() {
        return account;
    }

    public CryptoFacilitiesFill[] getFills() {
        return fills;
    }

    @Override
    public String toString() {
        return "CryptoFacilitiesFillsMessage{" +
                "feed='" + feed + '\'' +
                ", username='" + username + '\'' +
                ", account='" + account + '\'' +
                ", fills=" + Arrays.toString(fills) +
                '}';
    }
}
