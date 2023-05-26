package com.knowm.xchange.vertex.dto;

public class VertexPlaceOrder {
    private final long product_id;
    private final VertexOrder order;
    private final String signature;

    public VertexPlaceOrder(long productId, VertexOrder order, String signature) {
        product_id = productId;
        this.order = order;
        this.signature = signature;
    }

    public long getProduct_id() {
        return product_id;
    }

    public VertexOrder getOrder() {
        return order;
    }

    public String getSignature() {
        return signature;
    }
}
