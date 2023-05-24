package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowm.xchange.vertex.NanoSecondsDeserializer;

import java.time.Instant;


public class VertexMarketDataUpdateMessage {

    public static final Object[][] EMPTY_EVENTS = new Object[0][0];
    public static final VertexMarketDataUpdateMessage NULL = new VertexMarketDataUpdateMessage(EMPTY_EVENTS, EMPTY_EVENTS, null, null, null, null);

    private final Object[][] bids;
    private final Object[][] asks;
    private final Instant minTime;
    private final Instant maxTime;
    private final Instant lastMaxTime;
    private final String productId;

    public VertexMarketDataUpdateMessage(@JsonProperty("bids") Object[][] bids,
                                         @JsonProperty("asks") Object[][] asks,
                                         @JsonProperty("min_timestamp") @JsonDeserialize(using = NanoSecondsDeserializer.class) Instant minTime,
                                         @JsonProperty("max_timestamp") @JsonDeserialize(using = NanoSecondsDeserializer.class) Instant maxTime,
                                         @JsonProperty("last_max_timestamp") @JsonDeserialize(using = NanoSecondsDeserializer.class) Instant lastMaxTime,
                                         @JsonProperty("product_id") String productId

    ) {
        this.bids = bids;
        this.asks = asks;
        this.minTime = minTime;
        this.maxTime = maxTime;
        this.lastMaxTime = lastMaxTime;
        this.productId = productId;
    }


    public static VertexMarketDataUpdateMessage empty() {
        return new VertexMarketDataUpdateMessage(EMPTY_EVENTS, EMPTY_EVENTS, null, null, null, null);
    }

    public Object[][] getBids() {
        return bids;
    }

    public Object[][] getAsks() {
        return asks;
    }

    public Instant getMinTime() {
        return minTime;
    }

    public Instant getMaxTime() {
        return maxTime;
    }

    public Instant getLastMaxTime() {
        return lastMaxTime;
    }

    public String getProductId() {
        return productId;
    }
}
