package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.marketdata.BinanceBookTicker;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;

public class BookTickerBinanceWebSocketTransaction extends ProductBinanceWebSocketTransaction {

  private final BinanceBookTicker ticker;

  public BookTickerBinanceWebSocketTransaction(
      @JsonProperty("e") String eventType,
      @JsonProperty("E") String eventTime,
      @JsonProperty("T") String transactTime,
      @JsonProperty("u") long updateId,
      @JsonProperty("s") String symbol,
      @JsonProperty("b") BigDecimal bidPrice,
      @JsonProperty("B") BigDecimal bidQty,
      @JsonProperty("a") BigDecimal askPrice,
      @JsonProperty("A") BigDecimal askQty) {
    super(eventType, eventTime, transactTime, symbol);
    ticker = new BinanceBookTicker(getEventTime(), bidPrice, bidQty, askPrice, askQty, symbol);
    ticker.setUpdateId(updateId);
    ticker.setCurrencyPair(BinanceAdapters.adaptSymbol(symbol));
  }

  public CurrencyPair getCurrencyPair() {
    return ticker.getCurrencyPair();
  }

  public BinanceBookTicker getTicker() {
    return ticker;
  }
}
