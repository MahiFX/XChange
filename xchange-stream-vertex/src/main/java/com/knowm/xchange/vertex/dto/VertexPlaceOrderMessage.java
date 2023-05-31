package com.knowm.xchange.vertex.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VertexPlaceOrderMessage implements VertexRequest {

    private final VertexPlaceOrder place_order;

    public VertexPlaceOrderMessage(VertexPlaceOrder placeOrder) {
        place_order = placeOrder;
    }


    @JsonIgnore
    @Override
    public String getSignature() {
        return place_order.getSignature();
    }
}
