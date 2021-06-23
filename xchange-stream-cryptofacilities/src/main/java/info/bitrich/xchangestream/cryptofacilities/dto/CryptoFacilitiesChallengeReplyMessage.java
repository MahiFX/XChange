package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;

/**
 * @author pchertalev
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CryptoFacilitiesChallengeReplyMessage extends CryptoFacilitiesEvent {
    @JsonProperty
    private final String message;

    @JsonCreator
    public CryptoFacilitiesChallengeReplyMessage(@JsonProperty("event") CryptoFacilitiesEventType event, @JsonProperty("message") String message) {
        super(event);
        this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
}
