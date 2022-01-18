package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitAuthParams {
    private final String clientId;
    private final long timestamp;
    private final String signature;
    private final String nonce;
    private final String data;

    public DeribitAuthParams(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("signature") String signature,
            @JsonProperty("nonce") String nonce,
            @JsonProperty("data") String data) {
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.signature = signature;
        this.nonce = nonce;
        this.data = data;
    }

    @JsonProperty("grant_type")
    public String getGrantType() {
        return "client_signature";
    }

    @JsonProperty("client_id")
    public String getClientId() {
        return clientId;
    }

    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    @JsonProperty("signature")
    public String getSignature() {
        return signature;
    }

    @JsonProperty("nonce")
    public String getNonce() {
        return nonce;
    }

    @JsonProperty("data")
    public String getData() {
        return data;
    }
}
