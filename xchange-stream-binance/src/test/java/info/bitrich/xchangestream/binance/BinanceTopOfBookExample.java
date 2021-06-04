package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceTopOfBookExample {

  private static final Logger LOG = LoggerFactory.getLogger(BinanceTopOfBookExample.class);

  public static void main(String[] args) throws InterruptedException {

    ExchangeSpecification spec =
        StreamingExchangeFactory.INSTANCE
            .createExchange(BinanceStreamingExchange.class)
            .getDefaultExchangeSpecification();
    BinanceStreamingExchange exchange =
        (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

    // First, we subscribe only for one currency pair at connection time (minimum requirement)
    ProductSubscription subscription =
        ProductSubscription.create()
            .addOrderbook(CurrencyPair.BTC_USDT)
            .build();
    // Note: at connection time, the live subscription is disabled
    exchange.connect(subscription).blockingAwait();

    Disposable orderBooksBtc =
        exchange
            .getStreamingMarketDataService()
            .getOrderBook(CurrencyPair.BTC_USDT)
            .doOnDispose(
                () ->
                    exchange
                        .getStreamingMarketDataService()
                        .unsubscribe(CurrencyPair.BTC_USDT, BinanceSubscriptionType.DEPTH))
            .subscribe(
                orderBook -> {
                  LOG.info("Top of Book: {} / {}", orderBook.getBids().stream().findFirst(), orderBook.getAsks().stream().findFirst());
                });

    Thread.sleep(Long.MAX_VALUE);

  }
}
