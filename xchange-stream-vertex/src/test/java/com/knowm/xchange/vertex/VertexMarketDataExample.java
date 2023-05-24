package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class VertexMarketDataExample {

    private static final Logger logger = LoggerFactory.getLogger(VertexMarketDataExample.class);

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
                .createExchange(VertexStreamingExchange.class)
                .getDefaultExchangeSpecification();

        exchangeSpecification.setApiKey("YOUR_CLIENT_ID");
        exchangeSpecification.setSecretKey("YOUR_CLIENT_SECRET");

        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);

        StreamingExchange deribitStreamingExchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        deribitStreamingExchange.connect().blockingAwait();

        subscribe(deribitStreamingExchange.getStreamingMarketDataService(), "wBTC-USDC");
        subscribe(deribitStreamingExchange.getStreamingMarketDataService(), "wETH-USDC");

        Thread.sleep(Long.MAX_VALUE);
    }

    public static void subscribe(StreamingMarketDataService streamingMarketDataService, String instrument) {
        CurrencyPair currencyPair = new CurrencyPair(instrument);

        Observable<OrderBook> orderBook = streamingMarketDataService.getOrderBook(currencyPair);

        AtomicLong counter = new AtomicLong(0);
        orderBook.subscribe(book -> logger.info("Received book update for instrument {}: #{} {}", instrument, counter.incrementAndGet(), book));
    }
}
