package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.knowm.xchange.binance.dto.marketdata.BinanceBookTicker;

import java.math.BigDecimal;

public class BookTickerBinanceWebsocketTransaction extends ProductBinanceWebSocketTransaction {

  private final BinanceBookTicker ticker;

  public BookTickerBinanceWebsocketTransaction(
      @JsonProperty("e") String eventType,
      @JsonProperty("u") long updateId,
      @JsonProperty("E") String eventTime,
      @JsonProperty("T") String transactTime,
      @JsonProperty("s") String symbol,
      @JsonProperty("b") BigDecimal bidPrice,
      @JsonProperty("B") BigDecimal bidQty,
      @JsonProperty("a") BigDecimal askPrice,
      @JsonProperty("A") BigDecimal askQty) {

    super(eventType, eventTime, transactTime, symbol);

    ticker =
        new BinanceBookTicker(
            bidPrice,
            bidQty,
            askPrice,
            askQty,
            getTransactTime(),
            symbol);
    ticker.setCurrencyPair(currencyPair);
  }

  public BinanceBookTicker getTicker() {
    return ticker;
  }
}
