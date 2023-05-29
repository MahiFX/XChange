package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VertexCancelOrdersMessage {

    private final CancelOrders cancelOrders;

    public VertexCancelOrdersMessage(@JsonProperty("cancel_orders") CancelOrders cancelOrders) {
        this.cancelOrders = cancelOrders;
    }

    public CancelOrders getCancelOrders() {
        return cancelOrders;
    }
}


