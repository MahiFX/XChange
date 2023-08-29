package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceDapiTopOfBookExample {

  private static final Logger LOG = LoggerFactory.getLogger(BinanceDapiTopOfBookExample.class);

  public static void main(String[] args) throws InterruptedException {
    ExchangeSpecification spec = StreamingExchangeFactory.INSTANCE
        .createExchange(BinanceCoinFuturesStreamingExchange.class)
        .getDefaultExchangeSpecification();
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER, true);
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY, true);
    //spec.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);
    //spec.setExchangeSpecificParametersItem("Use_Beta", true);
    //spec.setSslUri(WS_COIN_FUTURES_API_BASE_URI);

    BinanceFuturesStreamingExchange exchange = (BinanceFuturesStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

    CurrencyPair pair = new CurrencyPair("BTC/USD_PERP");

    // First, we subscribe only for one currency pair at connection time (minimum requirement)
    ProductSubscription subscription =
        ProductSubscription.create()
            .addOrderbook(pair)
            .build();

    // Note: at connection time, the live subscription is disabled
    exchange.connect(subscription).blockingAwait();

    exchange
        .getStreamingMarketDataService()
        .getOrderBook(pair)
        .subscribe(
            orderBook -> {
              LOG.error("Top of Book: {} / {}", orderBook.getBids().stream().findFirst(), orderBook.getAsks().stream().findFirst());
            });

    Thread.sleep(Long.MAX_VALUE);

  }
}
