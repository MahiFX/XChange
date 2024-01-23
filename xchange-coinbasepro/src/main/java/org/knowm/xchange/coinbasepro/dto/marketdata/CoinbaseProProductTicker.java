package org.knowm.xchange.coinbasepro.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class CoinbaseProProductTicker {

  private final String tradeId;
  private final BigDecimal price;
  private final BigDecimal size;
  private final BigDecimal bid;
  private final BigDecimal best_bid_size;
  private final BigDecimal ask;
  private final BigDecimal best_ask_size;
  private final BigDecimal volume;
  private final String time;

  public CoinbaseProProductTicker(
      @JsonProperty("trade_id") String tradeId,
      @JsonProperty("price") BigDecimal price,
      @JsonProperty("size") BigDecimal size,
      @JsonProperty("bid") BigDecimal bid,
      @JsonProperty("best_bid_size") BigDecimal best_bid_size,
      @JsonProperty("ask") BigDecimal ask,
      @JsonProperty("best_ask_size") BigDecimal best_ask_size,
      @JsonProperty("volume") BigDecimal volume,
      @JsonProperty("time") String time) {

    this.tradeId = tradeId;
    this.price = price;
    this.size = size;
    this.bid = bid;
    this.best_bid_size = best_bid_size;
    this.ask = ask;
    this.best_ask_size = best_ask_size;
    this.volume = volume;
    this.time = time;
  }

  public String getTradeId() {
    return tradeId;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getSize() {
    return size;
  }

  public BigDecimal getBid() {
    return bid;
  }

  public BigDecimal getBest_bid_size() {
    return best_bid_size;
  }

  public BigDecimal getAsk() {
    return ask;
  }

  public BigDecimal getBest_ask_size() {
    return best_ask_size;
  }

  public BigDecimal getVolume() {
    return volume;
  }

  public String getTime() {
    return time;
  }

  @Override
  public String toString() {
    return "CoinbaseProProductTicker [tradeId="
        + tradeId
        + ", price="
        + price
        + ", size="
        + size
        + ", bid="
        + bid
        + ", ask="
        + ask
        + ", best_bid_size="
        + best_bid_size
        + ", best_ask_size="
        + best_ask_size
        + ", volume="
        + volume
        + ", time="
        + time
        + "]";
  }
}
