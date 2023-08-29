package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceDapiTickerExample {

  private static final Logger LOG = LoggerFactory.getLogger(BinanceDapiTickerExample.class);

  public static void main(String[] args) throws InterruptedException {
    ExchangeSpecification spec = StreamingExchangeFactory.INSTANCE
        .createExchange(BinanceCoinFuturesStreamingExchange.class)
        .getDefaultExchangeSpecification();

    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER, true);
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY, true);
//        spec.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, false);
//        spec.setExchangeSpecificParametersItem("Use_Beta", false);
//        spec.setSslUri(WS_COIN_FUTURES_API_BASE_URI);

    BinanceFuturesStreamingExchange exchange = (BinanceFuturesStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

    Instrument pair = new CurrencyPair("BTC/USD_PERP");

    // First, we subscribe only for one currency pair at connection time (minimum requirement)
    ProductSubscription subscription =
        ProductSubscription.create()
            .addTicker(pair)
            .build();

    // Note: at connection time, the live subscription is disabled
    exchange.connect(subscription).blockingAwait();

    exchange
        .getStreamingMarketDataService()
        .getTicker(pair)
        .subscribe(
            ticker -> {
              LOG.error("Top of Book: {} / {} -> {}", ticker.getBid(), ticker.getAsk(), ticker);
            });

    Thread.sleep(Long.MAX_VALUE);

  }
}
