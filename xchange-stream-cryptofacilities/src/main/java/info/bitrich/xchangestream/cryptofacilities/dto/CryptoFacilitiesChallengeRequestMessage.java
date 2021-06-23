package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;

/**
 * @author pchertalev
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CryptoFacilitiesChallengeRequestMessage extends CryptoFacilitiesEvent {
    @JsonProperty
    private final String apiKey;

    @JsonCreator
    public CryptoFacilitiesChallengeRequestMessage(@JsonProperty("event") CryptoFacilitiesEventType event, @JsonProperty("api_key") String apiKey) {
        super(event);
        this.apiKey = apiKey;
    }

    @JsonProperty("api_key")
    public String getApiKey() {
        return apiKey;
    }
}
