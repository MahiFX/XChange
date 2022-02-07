package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class DeribitMarketDataUpdateMessage {
    public static final DeribitMarketDataUpdateMessage NULL = new DeribitMarketDataUpdateMessage(null, null, null, null, null, null, null);

    private final Object[][] bids;
    private final Object[][] asks;
    private final Long changeId;
    private final String instrumentName;
    private final Long prevChangeId;
    private final Date timestamp;
    private final String type;

    public DeribitMarketDataUpdateMessage(
            @JsonProperty("bids") Object[][] bids,
            @JsonProperty("asks") Object[][] asks,
            @JsonProperty("change_id") Long changeId,
            @JsonProperty("instrument_name") String instrumentName,
            @JsonProperty("prev_change_id") Long prevChangeId,
            @JsonProperty("timestamp") Date timestamp,
            @JsonProperty("type") String type) {
        this.bids = bids;
        this.asks = asks;
        this.changeId = changeId;
        this.instrumentName = instrumentName;
        this.prevChangeId = prevChangeId;
        this.timestamp = timestamp;
        this.type = type;
    }

    @JsonProperty("bids")
    public Object[][] getBids() {
        return bids;
    }

    @JsonProperty("asks")
    public Object[][] getAsks() {
        return asks;
    }

    @JsonProperty("change_id")
    public Long getChangeId() {
        return changeId;
    }

    @JsonProperty("instrument_name")
    public String getInstrumentName() {
        return instrumentName;
    }

    @JsonProperty("prev_change_id")
    public Long getPrevChangeId() {
        return prevChangeId;
    }

    @JsonProperty("timestamp")
    public Date getTimestamp() {
        return timestamp;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonIgnore
    public static DeribitMarketDataUpdateMessage empty(Date timestamp) {
        return new DeribitMarketDataUpdateMessage(null, null, null, null, null, timestamp, null);
    }
}
