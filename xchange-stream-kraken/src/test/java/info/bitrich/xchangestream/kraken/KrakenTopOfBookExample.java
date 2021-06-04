package info.bitrich.xchangestream.kraken;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class KrakenTopOfBookExample {

    private static final Logger LOG = LoggerFactory.getLogger(KrakenTopOfBookExample.class);

    public static void main(String[] args) throws InterruptedException {

        ExchangeSpecification exchangeSpecification =
                new ExchangeSpecification(KrakenStreamingExchange.class);

        StreamingExchange krakenExchange =
                StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
        krakenExchange.connect().blockingAwait();

        Disposable tickerDis =
                krakenExchange
                        .getStreamingMarketDataService()
                        .getOrderBook(CurrencyPair.BTC_USD)
                        .subscribe(
                                s -> {
                                    LOG.info("Top of Book: {} / {}", s.getBids().stream().findFirst(), s.getAsks().stream().findFirst());
                                },
                                throwable -> {
                                    LOG.error("Fail to get ticker {}", throwable.getMessage(), throwable);
                                });

        TimeUnit.SECONDS.sleep(600);

        tickerDis.dispose();

        krakenExchange.disconnect().subscribe(() -> LOG.info("Disconnected"));
    }
}
