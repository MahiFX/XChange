package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class VertexOrderExample {

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException, InterruptedException {

        ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
                .createExchangeWithoutSpecification(VertexStreamingExchange.class)
                .getDefaultExchangeSpecification();

        // generate key pair
        ECKeyPair ecKeyPair = Keys.createEcKeyPair();
        String address = "0x" + Keys.getAddress(ecKeyPair.getPublicKey());

        exchangeSpecification.setApiKey(address);
        exchangeSpecification.setSecretKey(Numeric.toHexStringNoPrefix(ecKeyPair.getPrivateKey()));
        exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);

        VertexStreamingExchange exchange = (VertexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

        VertexStreamingTradeService tradeService = exchange.getStreamingTradeService();

        MarketOrder order = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), new CurrencyPair("wBTC-USDC"));
        tradeService.placeMarketOrder(order);

        Thread.sleep(60000);

        System.exit(0);


    }
}
