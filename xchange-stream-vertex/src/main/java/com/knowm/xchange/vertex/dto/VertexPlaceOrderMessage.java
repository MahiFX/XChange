package com.knowm.xchange.vertex.dto;

public class VertexPlaceOrderMessage {

    private final VertexPlaceOrder place_order;

    public VertexPlaceOrderMessage(VertexPlaceOrder placeOrder) {
        place_order = placeOrder;
    }

    public VertexPlaceOrder getPlace_order() {
        return place_order;
    }
}
