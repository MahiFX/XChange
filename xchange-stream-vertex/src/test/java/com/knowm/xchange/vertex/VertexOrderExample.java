package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
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
        exchangeSpecification.setUserName("default"); //subaccount name

        VertexStreamingExchange exchange = (VertexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        VertexStreamingTradeService tradeService = exchange.getStreamingTradeService();

        MarketOrder buy = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), new CurrencyPair("wBTC-USDC"));
        buy.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_IOC);
        tradeService.placeMarketOrder(buy);

        Thread.sleep(1000);

        MarketOrder sell = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), new CurrencyPair("wBTC-USDC"));
        sell.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_FOK);
        tradeService.placeMarketOrder(sell);

        LimitOrder resting = new LimitOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), new CurrencyPair("wBTC-USDC"), null, null, BigDecimal.valueOf(27000));
        tradeService.placeLimitOrder(resting);

        System.exit(0);


    }
}
