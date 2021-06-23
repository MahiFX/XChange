package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

public class CryptoFacilitiesOpenOrder {

  private final String instrument;
  private final Date time;
  private final Date lastUpdate;
  private final BigDecimal size;
  private final BigDecimal filledSize;
  private final BigDecimal limitPrice;
  private final BigDecimal stopPrice;
  private final String orderType;
  private final String orderId;
  private final String clientOrderId;
  private final int side;

  public CryptoFacilitiesOpenOrder(
          @JsonProperty("instrument") String instrument,
          @JsonProperty("time") long time,
          @JsonProperty("last_update_time") long lastUpdate,
          @JsonProperty("qty") BigDecimal size,
          @JsonProperty("filled") BigDecimal filledSize,
          @JsonProperty("limit_price") BigDecimal limitPrice,
          @JsonProperty("stop_price") BigDecimal stopPrice,
          @JsonProperty("type") String orderType,
          @JsonProperty("order_id") String orderId,
          @JsonProperty("cli_ord_id") String clientOrderId,
          @JsonProperty("direction") int side) {
    this.instrument = instrument;
    this.time = new Date(time);
    this.lastUpdate = new Date(lastUpdate);
    this.size = size;
    this.filledSize = filledSize;
    this.limitPrice = limitPrice;
    this.stopPrice = stopPrice;
    this.orderType = orderType;
    this.orderId = orderId;
    this.clientOrderId = clientOrderId;
    this.side = side;
  }

  public String getInstrument() {
    return instrument;
  }

  public Date getTime() {
    return time;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public BigDecimal getSize() {
    return size;
  }

  public BigDecimal getFilledSize() {
    return filledSize;
  }

  public BigDecimal getLimitPrice() {
    return limitPrice;
  }

  public BigDecimal getStopPrice() {
    return stopPrice;
  }

  public String getOrderType() {
    return orderType;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getClientOrderId() {
    return clientOrderId;
  }

  public int getSide() {
    return side;
  }

  @Override
  public String toString() {
    return "CryptoFacilitiesOpenOrder{" +
            "instrument='" + instrument + '\'' +
            ", time=" + time +
            ", lastUpdate=" + lastUpdate +
            ", size=" + size +
            ", filledSize=" + filledSize +
            ", limitPrice=" + limitPrice +
            ", stopPrice=" + stopPrice +
            ", orderType='" + orderType + '\'' +
            ", orderId='" + orderId + '\'' +
            ", clientOrderId='" + clientOrderId + '\'' +
            ", side=" + side +
            '}';
  }
}
