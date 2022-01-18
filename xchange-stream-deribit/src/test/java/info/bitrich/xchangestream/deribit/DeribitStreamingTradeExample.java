package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class DeribitStreamingTradeExample {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingTradeExample.class);

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
                .createExchange(DeribitStreamingExchange.class)
                .getDefaultExchangeSpecification();

        exchangeSpecification.setApiKey("YOUR_CLIENT_ID");
        exchangeSpecification.setSecretKey("YOUR_CLIENT_SECRET");

        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);

        StreamingExchange deribitStreamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        deribitStreamingExchange.connect().blockingAwait();

        CurrencyPair currencyPair = new CurrencyPair("BTC-PERPETUAL");
        Observable<Trade> tradeStream = deribitStreamingExchange.getStreamingMarketDataService().getTrades(currencyPair);

        AtomicLong counter = new AtomicLong(0);
        tradeStream.subscribe(trade -> {
            logger.info("Received trade #{}: {}", counter.incrementAndGet(), trade);
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
