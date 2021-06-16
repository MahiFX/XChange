package info.bitrich.xchangestream.cryptofacilities;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CryptoFacilitiesTopOfBookExample {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoFacilitiesTopOfBookExample.class);

    public static void main(String[] args) throws InterruptedException {

        ExchangeSpecification exchangeSpecification =
                new ExchangeSpecification(info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingExchange.class);

        StreamingExchange krakenExchange =
                StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
        krakenExchange.connect().blockingAwait();

        Disposable tickerDis =
                krakenExchange
                        .getStreamingMarketDataService()
                        .getOrderBook(new CurrencyPair(Currency.getInstance("PI_XBTUSD"), new Currency("")))
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
