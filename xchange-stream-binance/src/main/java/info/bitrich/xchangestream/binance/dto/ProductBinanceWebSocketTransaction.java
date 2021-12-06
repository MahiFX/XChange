package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductBinanceWebSocketTransaction extends BaseBinanceWebSocketTransaction {

  protected final String symbol;

  public ProductBinanceWebSocketTransaction(
      @JsonProperty("e") String eventType,
      @JsonProperty("E") String eventTime,
      @JsonProperty("T") String transactTime,
      @JsonProperty("s") String symbol) {
    super(eventType, eventTime, transactTime);
    this.symbol = symbol;
  }

  public String getSymbol() {
    return symbol;
  }
}
