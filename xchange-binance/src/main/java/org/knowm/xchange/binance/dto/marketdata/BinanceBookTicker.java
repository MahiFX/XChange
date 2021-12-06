package org.knowm.xchange.binance.dto.marketdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;

import java.math.BigDecimal;
import java.util.Date;

public final class BinanceBookTicker  {

  private final BigDecimal bidPrice;
  private final BigDecimal bidQty;
  private final BigDecimal askPrice;
  private final BigDecimal askQty;
  private final Date timestamp;
  private final String symbol;

  // The curency pair that is unfortunately not returned in the response
  private CurrencyPair pair;

  // The cached ticker
  private Ticker ticker;

  public BinanceBookTicker(
          @JsonProperty("bidPrice") BigDecimal bidPrice,
          @JsonProperty("bidQty") BigDecimal bidQty,
          @JsonProperty("askPrice") BigDecimal askPrice,
          @JsonProperty("askQty") BigDecimal askQty,
          @JsonProperty("timestamp") Date timestamp,
          @JsonProperty("symbol") String symbol) {
    this.bidPrice = bidPrice;
    this.bidQty = bidQty;
    this.askPrice = askPrice;
    this.askQty = askQty;
    this.timestamp = timestamp;
    this.symbol = symbol;
  }

  public String getSymbol() {
    return symbol;
  }

  public CurrencyPair getCurrencyPair() {
    return pair;
  }

  public void setCurrencyPair(CurrencyPair pair) {
    this.pair = pair;
  }

  public BigDecimal getBidPrice() {
    return bidPrice;
  }

  public BigDecimal getBidQty() {
    return bidQty;
  }

  public BigDecimal getAskPrice() {
    return askPrice;
  }

  public BigDecimal getAskQty() {
    return askQty;
  }

  public synchronized Ticker toTicker() {
    CurrencyPair currencyPair = pair;
    if (currencyPair == null) {
      currencyPair = BinanceAdapters.adaptSymbol(symbol);
    }
    if (ticker == null) {
      ticker =
          new Ticker.Builder()
              .currencyPair(currencyPair)
              .ask(askPrice)
              .bid(bidPrice)
              .askSize(askQty)
              .bidSize(bidQty)
              .timestamp(timestamp)
              .build();
    }
    return ticker;
  }
}
