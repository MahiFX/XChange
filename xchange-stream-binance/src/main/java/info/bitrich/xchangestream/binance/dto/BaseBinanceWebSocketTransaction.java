package info.bitrich.xchangestream.binance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public class BaseBinanceWebSocketTransaction {


  public enum BinanceWebSocketTypes {
    DEPTH_UPDATE("depthUpdate"),
    TICKER_24_HR("24hrTicker"),
    BOOK_TICKER("bookTicker"),
    KLINE("kline"),
    AGG_TRADE("aggTrade"),
    TRADE("trade"),
    OUTBOUND_ACCOUNT_POSITION("outboundAccountPosition"),
    EXECUTION_REPORT("executionReport");

    private static Map<String, BinanceWebSocketTypes> serializedValues = Arrays.stream(values()).collect(ImmutableMap.toImmutableMap(BinanceWebSocketTypes::getSerializedValue, Function.identity()));

        /**
         * Get a type from the `type` string of a `ProductBinanceWebSocketTransaction`.
         *
         * @param value The string representation.
         * @return THe enum value.
         */
        public static BinanceWebSocketTypes fromTransactionValue(String value) {
            return serializedValues.get(value);
        }

    private String serializedValue;

    BinanceWebSocketTypes(String serializedValue) {
      this.serializedValue = serializedValue;
    }

    public String getSerializedValue() {
      return serializedValue;
    }
  }

  protected final BinanceWebSocketTypes eventType;
  protected final Date eventTime;
  protected final Date transactTime;


  public BaseBinanceWebSocketTransaction(
          @JsonProperty("e") String _eventType, @JsonProperty("E") String _eventTime, @JsonProperty("T") String _transactTime) {
    this(
            BinanceWebSocketTypes.fromTransactionValue(_eventType),
            StringUtils.isNotEmpty(_eventTime) ? new Date(Long.parseLong(_eventTime)) : new Date(), StringUtils.isNotEmpty(_transactTime) ? new Date(Long.parseLong(_transactTime)) : null);
  }

  BaseBinanceWebSocketTransaction(BinanceWebSocketTypes eventType, Date eventTime, Date transactTime1) {
    this.eventType = eventType;
    this.eventTime = eventTime;
    this.transactTime = transactTime1;

  }

  public BinanceWebSocketTypes getEventType() {
    return eventType;
  }

  public Date getEventTime() {
    return eventTime;
  }

  public Date getTransactTime() {
    return transactTime;
  }
}
