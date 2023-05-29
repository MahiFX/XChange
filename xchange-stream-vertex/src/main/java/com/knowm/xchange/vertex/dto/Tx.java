package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tx {

    private final String sender;
    private final long[] productIds;
    private final String[] digests;
    private final String nonce;

    public Tx(@JsonProperty("sender") String sender, @JsonProperty("productIds") long[] productIds, @JsonProperty("digests") String[] digests, @JsonProperty("nonce") String nonce) {
        this.sender = sender;
        this.productIds = productIds;
        this.digests = digests;
        this.nonce = nonce;
    }

    public String getSender() {
        return sender;
    }

    public long[] getProductIds() {
        return productIds;
    }

    public String[] getDigests() {
        return digests;
    }

    public String getNonce() {
        return nonce;
    }
}
