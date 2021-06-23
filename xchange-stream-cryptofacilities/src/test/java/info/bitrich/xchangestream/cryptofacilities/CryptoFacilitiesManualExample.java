package info.bitrich.xchangestream.cryptofacilities;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CryptoFacilitiesManualExample {
    private static final Logger LOG = LoggerFactory.getLogger(CryptoFacilitiesManualExample.class);

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification =
                new ExchangeSpecification(CryptoFacilitiesStreamingExchange.class);
        exchangeSpecification.setExchangeSpecificParametersItem("Use_Beta", Boolean.TRUE);
        exchangeSpecification.setApiKey(args[0]);
        exchangeSpecification.setSecretKey(args[1]);
//        exchangeSpecification.setUserName(args[2]);

        StreamingExchange cryptoFacilitiesExchange =
                StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
        cryptoFacilitiesExchange.connect().blockingAwait();

        LOG.info("Connected");

        cryptoFacilitiesExchange
                .getStreamingTradeService()
                .getUserTrades(null)
                .subscribe(
                        b -> {
                            LOG.info("Received userTrade {}", b);
                        },
                        throwable -> {
                            LOG.error("UserTrades FAILED {}", throwable.getMessage(), throwable);
                        });
        cryptoFacilitiesExchange
                .getStreamingTradeService()
                .getOrderChanges(null)
                .subscribe(
                        b -> {
                            LOG.info("Received orderChange {}", b);
                        },
                        throwable -> {
                            LOG.error("OrderChange FAILED {}", throwable.getMessage(), throwable);
                        });

        TimeUnit.MINUTES.sleep(30);

        cryptoFacilitiesExchange.disconnect().subscribe(() -> LOG.info("Disconnected"));
    }
}
