package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeribitOpenPositionParams {
    private final String currency;

    public DeribitOpenPositionParams(@JsonProperty("currency") String currency) {
        this.currency = currency;
    }

    @JsonProperty("currency")
    public String getCurrency() {
        return currency;
    }
}
