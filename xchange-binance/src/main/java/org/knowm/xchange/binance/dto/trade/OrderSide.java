package org.knowm.xchange.binance.dto.trade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public enum OrderSide {
  BUY,
  SELL;

  @JsonCreator
  public static OrderSide getOrderSide(String s) {
    try {
      return OrderSide.valueOf(s);
    } catch (Exception e) {
      throw new RuntimeException("Unknown order side " + s + ".");
    }
  }
}
