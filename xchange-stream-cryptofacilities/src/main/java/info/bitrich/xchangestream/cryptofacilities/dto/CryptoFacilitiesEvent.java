package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;

/** @author pchertalev */
public class CryptoFacilitiesEvent {

  @JsonProperty("event")
  private final CryptoFacilitiesEventType event;

  public CryptoFacilitiesEvent(CryptoFacilitiesEventType event) {
    this.event = event;
  }

  public CryptoFacilitiesEventType getEvent() {
    return event;
  }
}
