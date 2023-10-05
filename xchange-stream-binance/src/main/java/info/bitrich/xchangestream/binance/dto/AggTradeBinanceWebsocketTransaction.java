package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class AggTradeBinanceWebsocketTransaction extends ProductBinanceWebSocketTransaction {

  private final BinanceRawTrade rawTrade;

  public AggTradeBinanceWebsocketTransaction(
          @JsonProperty("e") String eventType,
          @JsonProperty("E") String eventTime,
          @JsonProperty("s") String symbol,
          @JsonProperty("a") long tradeId,
          @JsonProperty("p") BigDecimal price,
          @JsonProperty("q") BigDecimal quantity,
          @JsonProperty("f") long firstTradeId,
          @JsonProperty("l") long lastTradeId,
          @JsonProperty("T") long timestamp,
          @JsonProperty("m") boolean buyerMarketMaker) {
    super(eventType, eventTime, null, symbol);

    rawTrade =
            new BinanceRawTrade(
                    eventType,
                    eventTime,
                    symbol,
                    tradeId,
                    price,
                    quantity,
                    -1,
                    -1,
                    timestamp,
                    buyerMarketMaker,
                    false);
  }

  public BinanceRawTrade getRawTrade() {
    return rawTrade;
  }
}
