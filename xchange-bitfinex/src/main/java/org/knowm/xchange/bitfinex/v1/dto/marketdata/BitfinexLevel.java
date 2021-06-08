package org.knowm.xchange.bitfinex.v1.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

public class BitfinexLevel {

  private final BigDecimal price;
  private final BigDecimal amount;
  private final BigDecimal timestamp;

  /** Local timestamp at which this event was received */
  @JsonIgnore
  private transient final Date receivedTimestamp;

  /**
   * Constructor
   *
   * @param price
   * @param amount
   * @param timestamp
   */
  @JsonCreator
  public BitfinexLevel(
          @JsonProperty("price") BigDecimal price,
          @JsonProperty("amount") BigDecimal amount,
          @JsonProperty("timestamp") BigDecimal timestamp) {
    this(price, amount, timestamp, new Date());
  }

  /**
   * Constructor
   *
   * @param price
   * @param amount
   * @param timestamp
   * @param receivedTimestamp
   */
  public BitfinexLevel(
          BigDecimal price,
          BigDecimal amount,
          BigDecimal timestamp,
          Date receivedTimestamp) {

    this.price = price;
    this.amount = amount;
    this.timestamp = timestamp;
    this.receivedTimestamp = receivedTimestamp;
  }

  public BigDecimal getPrice() {

    return price;
  }

  public BigDecimal getAmount() {

    return amount;
  }

  public BigDecimal getTimestamp() {

    return timestamp;
  }

  /** Local timestamp at which this event was received */
  public Date getReceivedTimestamp() {
    return receivedTimestamp;
  }

  @Override
  public String toString() {

    return "BitfinexLevel [price="
        + price
        + ", amount="
        + amount
        + ", timestamp="
        + timestamp
        + "]";
  }
}
