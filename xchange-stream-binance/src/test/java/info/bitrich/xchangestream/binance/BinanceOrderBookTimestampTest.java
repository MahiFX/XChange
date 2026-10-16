package info.bitrich.xchangestream.binance;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.binance.dto.DepthBinanceWebSocketTransaction;
import java.util.ArrayList;
import java.util.Date;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;

public class BinanceOrderBookTimestampTest {
  private static final long EVENT_TIME = 1499404630606L;
  private static final long TRANSACT_TIME = 1499404630600L;
  private static final CurrencyPair PAIR = CurrencyPair.ETH_BTC;

  private static ObjectMapper mapper;

  @BeforeClass
  public static void setupClass() {
    JsonFactory jf = new JsonFactory();
    jf.enable(JsonParser.Feature.ALLOW_COMMENTS);
    mapper = new ObjectMapper(jf);
  }

  @Test
  public void spotDepthUpdateAdvancesBookTimestampToEventTime() throws Exception {
    // Spot depthUpdate carries "E" but no "T"
    OrderBook book = applyToSnapshot(depthUpdate(EVENT_TIME, null));
    assertEquals(EVENT_TIME, book.getTimeStamp().getTime());
  }

  @Test
  public void futuresDepthUpdateAdvancesBookTimestampToTransactTime() throws Exception {
    OrderBook book = applyToSnapshot(depthUpdate(EVENT_TIME, TRANSACT_TIME));
    assertEquals(TRANSACT_TIME, book.getTimeStamp().getTime());
  }

  private static OrderBook applyToSnapshot(DepthBinanceWebSocketTransaction depth) {
    OrderBook book = new OrderBook(new Date(EVENT_TIME - 60_000), new ArrayList<>(), new ArrayList<>());
    BinanceStreamingMarketDataService.extractOrderBookUpdates(PAIR, depth).forEach(book::update);
    return book;
  }

  private static DepthBinanceWebSocketTransaction depthUpdate(long eventTime, Long transactTime) throws Exception {
    String json = "{\"e\":\"depthUpdate\",\"E\":" + eventTime
        + (transactTime != null ? ",\"T\":" + transactTime : "")
        + ",\"s\":\"ETHBTC\",\"U\":7913450,\"u\":7913455"
        + ",\"b\":[[\"0.10376590\",\"59.15767010\"]]"
        + ",\"a\":[[\"0.10376586\",\"159.15767010\"]]}";
    return mapper.readValue(json, DepthBinanceWebSocketTransaction.class);
  }
}
