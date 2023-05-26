package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.knowm.xchange.vertex.NanoSecondsDeserializer;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Trade;

import java.math.BigInteger;
import java.time.Instant;

import static com.knowm.xchange.vertex.dto.VertexModelUtils.convertToDecimal;

public class VertexTradeData {


    private final Instant timestamp;
    private final String productId;

    private final BigInteger makerQty;
    private final BigInteger takerQty;
    private final BigInteger price;
    private final Boolean isTakerBuyer;

    public VertexTradeData(@JsonProperty("timestamp") @JsonDeserialize(using = NanoSecondsDeserializer.class) Instant timestamp,
                           @JsonProperty("product_id") String productId,
                           @JsonProperty("maker_qty") BigInteger makerQty,
                           @JsonProperty("taker_qty") BigInteger takerQty,
                           @JsonProperty("price") BigInteger price,
                           @JsonProperty("is_taker_buyer") Boolean isTakerBuyer) {
        this.timestamp = timestamp;
        this.productId = productId;
        this.makerQty = makerQty;
        this.takerQty = takerQty;
        this.price = price;
        this.isTakerBuyer = isTakerBuyer;
    }


    public Trade toTrade(CurrencyPair currencyPair) {
        Trade.Builder builder = new Trade.Builder()
                .instrument(currencyPair)
                .price(convertToDecimal(price))
                .originalAmount(convertToDecimal(takerQty))
                .type(isTakerBuyer ? Order.OrderType.BID : Order.OrderType.ASK);

        builder.originalAmount(convertToDecimal(takerQty));
        return builder.build();
    }
}
