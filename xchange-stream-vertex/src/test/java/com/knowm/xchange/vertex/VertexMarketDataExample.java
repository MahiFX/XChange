package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class VertexMarketDataExample {

    private static final Logger logger = LoggerFactory.getLogger(VertexMarketDataExample.class);
    public static final String BTC_USDC = "wBTC-USDC";

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(VertexStreamingExchange.class);

        exchangeSpecification.setApiKey("YOUR_WALLET_ADDRESS");
        exchangeSpecification.setSecretKey("YOUR_WALLET_SECRET_KEY");

        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);

        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        exchange.connect().blockingAwait();

        Disposable ticker = exchange.getStreamingMarketDataService().getTicker(new CurrencyPair(BTC_USDC))
                .forEach(tick -> {
                    logger.info(BTC_USDC + " TOB: " + tick);
                });

        Disposable disconnectBtcTOB = subscribe(exchange.getStreamingMarketDataService(), BTC_USDC, 1);
        Disposable disconnectBtc15 = subscribe(exchange.getStreamingMarketDataService(), BTC_USDC, 15);

        Disposable disconnectEth = subscribe(exchange.getStreamingMarketDataService(), "wETH-USDC", 2);

        Thread.sleep(10000);

        logger.info("\n\n Disconnecting 15 depth BTC-USDC \n\n");

        disconnectBtc15.dispose();

        logger.info("\n\n Disconnecting ETH-USDC \n\n");

        disconnectEth.dispose();

        Thread.sleep(10000);

        disconnectBtcTOB.dispose();

        ticker.dispose();

        exchange.disconnect().blockingAwait();


    }

    public static Disposable subscribe(StreamingMarketDataService streamingMarketDataService, String instrument, int depth) {
        CurrencyPair currencyPair = new CurrencyPair(instrument);

        Observable<OrderBook> orderBook = streamingMarketDataService.getOrderBook(currencyPair, depth);

        AtomicLong counter = new AtomicLong(0);
        Disposable disconnectMarketData = orderBook.subscribe(book -> logger.info("Received book update for instrument {}, depth {} #{} {}", instrument, depth, counter.incrementAndGet(), book));

        Observable<Trade> trades = streamingMarketDataService.getTrades(currencyPair);
        Disposable disconnectTrades = trades.subscribe(trade -> logger.info("Received trade update for instrument {}: {}", instrument, trade));


        return new CompositeDisposable(disconnectMarketData, disconnectTrades);
    }
}
