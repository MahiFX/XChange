package info.bitrich.xchangestream.deribit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class DerebitOrderParams {
    private final String instrument;
    private final BigDecimal amount;
    private final BigDecimal price;
    private final String type;
    private final String label;
    private final DeribitTimeInForce timeInForce;
    private final Boolean postOnly;

    public DerebitOrderParams(
            @JsonProperty("instrument_name") String instrument,
            @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("type") String type,
            @JsonProperty("label") String label,
            @JsonProperty("time_in_force") DeribitTimeInForce timeInForce,
            @JsonProperty("post_only") Boolean postOnly) {
        this.instrument = instrument;
        this.amount = amount;
        this.price = price;
        this.type = type;
        this.label = label;
        this.timeInForce = timeInForce;
        this.postOnly = postOnly;
    }

    @JsonProperty("instrument_name")
    public String getInstrument() {
        return instrument;
    }

    @JsonProperty("amount")
    public BigDecimal getAmount() {
        return amount;
    }

    @JsonProperty("price")
    public BigDecimal getPrice() {
        return price;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    @JsonProperty("time_in_force")
    public DeribitTimeInForce getTimeInForce() {
        return timeInForce;
    }

    @JsonProperty("post_only")
    public Boolean getPostOnly() {
        return postOnly;
    }
}
