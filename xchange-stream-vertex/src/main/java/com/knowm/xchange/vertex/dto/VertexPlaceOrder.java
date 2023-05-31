package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VertexPlaceOrder {
    private final long product_id;
    private final VertexOrder order;
    private final String signature;
    private final boolean spot_leverage;

    public VertexPlaceOrder(long productId, VertexOrder order, String signature, boolean spotLeverage) {
        product_id = productId;
        this.order = order;
        this.signature = signature;
        spot_leverage = spotLeverage;
    }

}
