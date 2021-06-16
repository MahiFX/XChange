package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesSubscriptionName;

import java.util.List;

/**
 * @author pchertalev
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CryptoFacilitiesSubscriptionMessage extends CryptoFacilitiesEvent {

    /** Optional, client originated ID reflected in response message. */
//  @JsonProperty private final Integer reqid;

    /**
     * Optional - Array of currency pairs. Format of each pair is "A/B", where A and B are ISO 4217-A3
     * for standardized assets and popular unique symbol if not standardized.
     */
    @JsonProperty(value = "product_ids", required = false)
    private final List<String> product_ids;

    @JsonProperty
    private final CryptoFacilitiesSubscriptionName feed;

    @JsonCreator
    public CryptoFacilitiesSubscriptionMessage(
            @JsonProperty("event") CryptoFacilitiesEventType event,
            @JsonProperty("feed") CryptoFacilitiesSubscriptionName feed,
            @JsonProperty("product_ids") List<String> product_ids) {
        super(event);
        this.feed = feed;
        this.product_ids = product_ids;
    }

    @JsonProperty(value = "product_ids", required = false)
    public List<String> getProduct_ids() {
        return product_ids;
    }

    public CryptoFacilitiesSubscriptionName getFeed() {
        return feed;
    }
}
