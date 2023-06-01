package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.trade.params.DefaultCancelOrderByInstrumentAndIdParams;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;

public class VertexOrderExample {

    public static void main(String[] args) throws IOException, InterruptedException {

        ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
                .createExchangeWithoutSpecification(VertexStreamingExchange.class)
                .getDefaultExchangeSpecification();


        ECKeyPair ecKeyPair = Credentials.create(System.getProperty("WALLET_PRIVATE_KEY")).getEcKeyPair();
        String address = "0x" + Keys.getAddress(ecKeyPair.getPublicKey());

        exchangeSpecification.setApiKey(address);
        exchangeSpecification.setSecretKey(Numeric.toHexStringNoPrefix(ecKeyPair.getPrivateKey()));
        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);
        exchangeSpecification.setExchangeSpecificParametersItem(VertexStreamingExchange.USE_LEVERAGE, true);
        exchangeSpecification.setUserName("default"); //subaccount name

        VertexStreamingExchange exchange = (VertexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        VertexStreamingTradeService tradeService = exchange.getStreamingTradeService();

        CurrencyPair btc = new CurrencyPair("wBTC-USDC");

        MarketOrder buy = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc);
        buy.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_IOC);
        tradeService.placeMarketOrder(buy);

        Thread.sleep(2000);

        MarketOrder sell = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), btc);
        sell.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_FOK);
        tradeService.placeMarketOrder(sell);

        LimitOrder resting = new LimitOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc, null, null, BigDecimal.valueOf(27000));
        String orderId = tradeService.placeLimitOrder(resting);

        Thread.sleep(5000);

        tradeService.cancelOrder(new DefaultCancelOrderByInstrumentAndIdParams(btc, orderId));

        // Check leveraged shorting works
        sell = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), btc);
        sell.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_FOK);
        tradeService.placeMarketOrder(sell);

        buy = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc);
        buy.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_IOC);
        tradeService.placeMarketOrder(buy);

        exchange.disconnect().blockingAwait();

    }
}
