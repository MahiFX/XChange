package com.knowm.xchange.vertex.signing.schemas;

import com.knowm.xchange.vertex.dto.VertexModelUtils;
import com.knowm.xchange.vertex.signing.EIP712Domain;
import com.knowm.xchange.vertex.signing.EIP712Schema;
import com.knowm.xchange.vertex.signing.EIP712Type;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.of;

public class PlaceOrderSchema extends EIP712Schema {

    private PlaceOrderSchema(EIP712Domain domain, Map<String, Object> message) {
        super(of("Order", List.of(
                        new EIP712Type("sender", "bytes32"),
                        new EIP712Type("priceX18", "int128"),
                        new EIP712Type("amount", "int128"),
                        new EIP712Type("expiration", "uint64"),
                        new EIP712Type("nonce", "uint64")
                )),
                "Order",
                domain,
                message);
    }

    public static PlaceOrderSchema build(Order order, String chainId, String verifyingContract, Long nonce, String sender) {
        EIP712Domain domain = getDomain(chainId, verifyingContract);

        BigInteger quantityAsInt = order.getOriginalAmount().multiply(VertexModelUtils.NUMBER_CONVERSION_FACTOR).toBigInteger();
        if (order.getType().equals(Order.OrderType.ASK)) {
            quantityAsInt = quantityAsInt.multiply(BigInteger.valueOf(-1));
        }

        //FIXME expiration / timeInForce

        Map<String, Object> fields = new HashMap<>(of(
                "sender", sender,
                "amount", quantityAsInt,
                "nonce", nonce,
                "expiration", BigInteger.ZERO));

        if (order instanceof LimitOrder) {
            BigInteger priceAsInt = ((LimitOrder) order).getLimitPrice().multiply(VertexModelUtils.NUMBER_CONVERSION_FACTOR).toBigInteger();
            fields.put("priceX18", priceAsInt);
        } else {
            fields.put("priceX18", BigInteger.ZERO);
        }
        PlaceOrderSchema placeOrderSchema = new PlaceOrderSchema(domain, fields);

        return placeOrderSchema;
    }

}
