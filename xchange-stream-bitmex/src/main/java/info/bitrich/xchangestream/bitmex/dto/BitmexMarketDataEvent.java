package info.bitrich.xchangestream.bitmex.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/** Created by Lukas Zaoralek on 13.11.17. */
public class BitmexMarketDataEvent {
  public static final String BITMEX_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  protected String timestamp;
  protected String symbol;

  /** Local timestamp at which this event was received */
  @JsonIgnore
  private transient final Date receivedTimestamp = new Date();

  public BitmexMarketDataEvent(String symbol, String timestamp) {
    this.timestamp = timestamp;
    this.symbol = symbol;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getSymbol() {
    return symbol;
  }

  /** Local timestamp at which this event was received */
  public Date getReceivedTimestamp() {
    return receivedTimestamp;
  }

  public CurrencyPair getCurrencyPair() {
    String base = symbol.substring(0, 3);
    String counter = symbol.substring(3);
    return new CurrencyPair(Currency.getInstance(base), Currency.getInstance(counter));
  }

  public Date getDate() {
    SimpleDateFormat formatter = new SimpleDateFormat(BITMEX_TIMESTAMP_FORMAT);
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date date = null;
    try {
      date = formatter.parse(timestamp);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return date;
  }
}
