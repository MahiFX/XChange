package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

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

        AtomicLong counter = new AtomicLong(0);
        orderBook.subscribe(book -> {
            logger.info("Received book update #" + counter.incrementAndGet());
        });

        Thread.sleep(Long.MAX_VALUE);
    }
}
