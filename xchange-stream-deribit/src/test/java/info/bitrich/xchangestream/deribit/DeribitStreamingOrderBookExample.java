package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeribitStreamingOrderBookExample {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingOrderBookExample.class);

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
                .createExchange(DeribitStreamingExchange.class)
                .getDefaultExchangeSpecification();

        StreamingExchange deribitStreamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        deribitStreamingExchange.connect().blockingAwait();

        CurrencyPair currencyPair = new CurrencyPair("BTC-PERPETUAL");
        Observable<OrderBook> orderBook = deribitStreamingExchange.getStreamingMarketDataService().getOrderBook(currencyPair);

        orderBook.subscribe(book -> {
            logger.info("Received book: " + book);
        });

        Thread.sleep(Long.MAX_VALUE);
    }
}
