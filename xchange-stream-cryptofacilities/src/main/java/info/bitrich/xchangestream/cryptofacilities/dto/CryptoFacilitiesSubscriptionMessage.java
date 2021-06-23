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

    @JsonProperty
    private final CryptoFacilitiesSubscriptionName feed;

    /**
     * Optional - Array of currency pairs. Format of each pair is "A/B", where A and B are ISO 4217-A3
     * for standardized assets and popular unique symbol if not standardized.
     */
    @JsonProperty(value = "product_ids")
    private final List<String> product_ids;

    @JsonProperty(value = "api_key")
    private final String api_key;

    @JsonProperty(value = "original_challenge")
    private final String original_challenge;

    @JsonProperty(value = "signed_challenge")
    private final String signed_challenge;

    @JsonCreator
    public CryptoFacilitiesSubscriptionMessage(
            @JsonProperty("event") CryptoFacilitiesEventType event,
            @JsonProperty("feed") CryptoFacilitiesSubscriptionName feed,
            @JsonProperty("product_ids") List<String> product_ids,
            @JsonProperty("api_key") String api_key,
            @JsonProperty("original_challenge") String original_challenge,
            @JsonProperty("signed_challenge") String signed_challenge) {
        super(event);
        this.feed = feed;
        this.product_ids = product_ids;
        this.api_key = api_key;
        this.original_challenge = original_challenge;
        this.signed_challenge = signed_challenge;
    }

    public CryptoFacilitiesSubscriptionName getFeed() {
        return feed;
    }

    @JsonProperty(value = "product_ids")
    public List<String> getProduct_ids() {
        return product_ids;
    }

    @JsonProperty(value = "api_key")
    public String getApiKey() {
        return api_key;
    }

    @JsonProperty("original_challenge")
    public String getOriginalChallenge() {
        return original_challenge;
    }

    @JsonProperty("signed_challenge")
    public String getSignedChallenge() {
        return signed_challenge;
    }
}
