package org.knowm.xchange.examples.vertex;

import com.knowm.xchange.vertex.VertexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.util.concurrent.atomic.AtomicLong;

public class VertexMarketDataExample {

    private static final Logger logger = LoggerFactory.getLogger(VertexMarketDataExample.class);


    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(VertexStreamingExchange.class);

        String privateKey = System.getProperty("WALLET_PRIVATE_KEY");
        ECKeyPair ecKeyPair = Credentials.create(privateKey).getEcKeyPair();
        String address = "0x" + Keys.getAddress(ecKeyPair.getPublicKey());

        exchangeSpecification.setApiKey(address);
        exchangeSpecification.setSecretKey(privateKey);

        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);
        exchangeSpecification.setExchangeSpecificParametersItem(VertexStreamingExchange.SECONDARY_SUBSCRIPTIONS_WEBSOCKET, "wss://gateway.sei-test.vertexprotocol.com/v1/subscribe,wss://gateway.sepolia-test.vertexprotocol.com/v1/subscribe");
        exchangeSpecification.setExchangeSpecificParametersItem(VertexStreamingExchange.SECONDARY_CUSTOM_HOSTS, ",gateway.mantle-test.vertexprotocol.com");

        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        if (exchange.isAlive()) {
            throw new RuntimeException("Exchange is already alive, should not be connected yet.");
        }

        exchange.connect().blockingAwait();

        if (!exchange.isAlive()) {
            throw new RuntimeException("Exchange should be alive after connect.");
        }

        CurrencyPair btcUsdc = new CurrencyPair(Currency.BTC, Currency.USDC);
        CurrencyPair ethUsdc = new CurrencyPair(Currency.ETH, Currency.USDC);

        Disposable ticker = exchange.getStreamingMarketDataService().getTicker(btcUsdc)
                .forEach(tick -> logger.info(btcUsdc + " TOB: " + tick));

        Disposable disconnectBtcTOB = subscribe(exchange.getStreamingMarketDataService(), btcUsdc.toString(), 1);
        Disposable disconnectBtc15 = subscribe(exchange.getStreamingMarketDataService(), btcUsdc.toString(), 15);

        Disposable disconnectEth = subscribe(exchange.getStreamingMarketDataService(), ethUsdc.toString(), 2);

        logger.info("\n\n Disconnecting 15 depth BTC-USDC \n\n");

        disconnectBtc15.dispose();

        if (!exchange.isAlive()) {
            throw new RuntimeException("Exchange should be alive after connect.");
        }

        logger.info("\n\n Disconnecting ETH-USDC \n\n");

        disconnectEth.dispose();

        Thread.sleep(10000);

        disconnectBtcTOB.dispose();

        if (!exchange.isAlive()) {
            throw new RuntimeException("Exchange should be alive after connect.");
        }

        ticker.dispose();

        exchange.disconnect().blockingAwait();

        if (exchange.isAlive()) {
            throw new RuntimeException("Exchange should be disconnected");
        }


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
