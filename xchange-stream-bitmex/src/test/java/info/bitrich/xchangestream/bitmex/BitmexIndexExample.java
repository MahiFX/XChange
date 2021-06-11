package info.bitrich.xchangestream.bitmex;

import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitmexIndexExample {

  private static final Logger LOG = LoggerFactory.getLogger(BitmexIndexExample.class);

  public static void main(String[] args) {
    BitmexStreamingExchange exchange =
        (BitmexStreamingExchange)
            StreamingExchangeFactory.INSTANCE.createExchange(BitmexStreamingExchange.class);
    exchange.connect().blockingAwait();

    final BitmexStreamingMarketDataService streamingMarketDataService =
        (BitmexStreamingMarketDataService) exchange.getStreamingMarketDataService();

    streamingMarketDataService.getRawInstrument(".BXBT")
            .subscribe(
                    ticker -> {
                        LOG.info("INSTRUMENT: {}", ticker);
                    },
                    throwable -> LOG.error("ERROR in getting ticker: ", throwable));


    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    exchange.disconnect().blockingAwait();
  }
}
