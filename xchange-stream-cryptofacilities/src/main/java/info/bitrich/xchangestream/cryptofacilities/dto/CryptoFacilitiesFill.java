package info.bitrich.xchangestream.cryptofacilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

public class CryptoFacilitiesFill {

  private final String instrument;
  private final Date time;
  private final BigDecimal price;
  private final int seq;
  private final boolean buy;
  private final BigDecimal size;
  private final String orderId;
  private final String fillId;
  private final String fillType;
  private final BigDecimal feePaid;
  private final String feeCurrency;

  public CryptoFacilitiesFill(
          @JsonProperty("instrument") String instrument,
          @JsonProperty("time") long time,
          @JsonProperty("price") BigDecimal price,
          @JsonProperty("seq") int seq,
          @JsonProperty("buy") boolean buy,
          @JsonProperty("qty") BigDecimal size,
          @JsonProperty("order_id") String orderId,
          @JsonProperty("fill_id") String fillId,
          @JsonProperty("fill_type") String fillType,
          @JsonProperty("fee_paid") BigDecimal feePaid,
          @JsonProperty("fee_currency") String feeCurrency) {
    this.instrument = instrument;
    this.time = new Date(time);
    this.price = price;
    this.seq = seq;
    this.buy = buy;
    this.size = size;
    this.orderId = orderId;
    this.fillId = fillId;
    this.fillType = fillType;
    this.feePaid = feePaid;
    this.feeCurrency = feeCurrency;
  }

  public String getInstrument() {
    return instrument;
  }

  public Date getTime() {
    return time;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getSeq() {
    return seq;
  }

  public boolean isBuy() {
    return buy;
  }

  public BigDecimal getSize() {
    return size;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getFillId() {
    return fillId;
  }

  public String getFillType() {
    return fillType;
  }

  public BigDecimal getFeePaid() {
    return feePaid;
  }

  public String getFeeCurrency() {
    return feeCurrency;
  }

  @Override
  public String toString() {
    return "CryptoFacilitiesFill{" +
            "instrument='" + instrument + '\'' +
            ", time=" + time +
            ", price=" + price +
            ", seq=" + seq +
            ", buy=" + buy +
            ", size=" + size +
            ", orderId='" + orderId + '\'' +
            ", fillId='" + fillId + '\'' +
            ", fillType='" + fillType + '\'' +
            ", feePaid=" + feePaid +
            ", feeCurrency='" + feeCurrency + '\'' +
            '}';
  }
}
