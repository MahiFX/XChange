package com.knowm.xchange.vertex.dto;

public class VertexOrderMessage {

    private final VertexPlaceOrder place_order;

    public VertexOrderMessage(VertexPlaceOrder placeOrder) {
        place_order = placeOrder;
    }

    public VertexPlaceOrder getPlace_order() {
        return place_order;
    }
}
