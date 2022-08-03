package org.knowm.xchange.binance.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.dto.marketdata.Ticker;

import java.math.BigDecimal;
import java.util.Date;

@Getter
public final class BinanceBookTicker {
  private final Date timestamp;
  private final Date creationTimestamp = new Date();
  public long updateId;
  private final BigDecimal bidPrice;
  private final BigDecimal bidQty;
  private final BigDecimal askPrice;
  private final BigDecimal askQty;
  private final String symbol;

  // The cached ticker
  private Ticker ticker;

  public BinanceBookTicker(
          @JsonProperty("timestamp") Date timestamp,
          @JsonProperty("bidPrice") BigDecimal bidPrice,
          @JsonProperty("bidQty") BigDecimal bidQty,
          @JsonProperty("askPrice") BigDecimal askPrice,
          @JsonProperty("askQty") BigDecimal askQty,
          @JsonProperty("symbol") String symbol) {
    this.timestamp = timestamp;
    this.bidPrice = bidPrice;
    this.bidQty = bidQty;
    this.askPrice = askPrice;
    this.askQty = askQty;
    this.symbol = symbol;
  }

  public void setUpdateId(long updateId) {
    this.updateId = updateId;
  }

  public synchronized Ticker toTicker(boolean isFuture) {
    if (ticker == null) {
      ticker =
              new Ticker.Builder()
                      .instrument(BinanceAdapters.adaptSymbol(symbol, isFuture))
                      .timestamp(timestamp)
                      .creationTimestamp(creationTimestamp)
              .ask(askPrice)
              .bid(bidPrice)
              .askSize(askQty)
              .bidSize(bidQty)
              .build();
    }
    return ticker;
  }
}
