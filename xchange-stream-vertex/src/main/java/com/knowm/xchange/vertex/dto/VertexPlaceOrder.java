package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VertexPlaceOrder {
    private final long product_id;
    private final VertexOrder order;
    private final String signature;

    public VertexPlaceOrder(long productId, VertexOrder order, String signature) {
        product_id = productId;
        this.order = order;
        this.signature = signature;
    }

}
