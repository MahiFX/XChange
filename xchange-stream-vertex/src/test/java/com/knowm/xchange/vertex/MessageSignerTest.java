package com.knowm.xchange.vertex;

import com.knowm.xchange.vertex.dto.VertexModelUtils;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.schemas.PlaceOrderSchema;
import org.junit.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;

public class MessageSignerTest {

    Logger log = LoggerFactory.getLogger(MessageSignerTest.class);

    @Test
    public void testSignOrder() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {

        ECKeyPair ecKeyPair = Keys.createEcKeyPair();

        MessageSigner messageSigner = new MessageSigner(ecKeyPair.getPrivateKey().toString(16));

        String orderId = "gogogogogogo";
        Order order = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(1000.0), new CurrencyPair("WBTC/USDC"), null, new Date(), null, null, null, null, orderId);


        PlaceOrderSchema orderSchema = PlaceOrderSchema.build(order, "421613", "0xf03f457a30e598d5020164a339727ef40f2b8fbc", 1L, VertexModelUtils.buildSender("0x3cd04f7Dbef1DE0C27100536CE12819Ee9dCFAC3", ""));
        String signatureData = messageSigner.signMessage(orderSchema);

        log.info("signatureData: {}", signatureData);


    }
}
