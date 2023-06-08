package org.knowm.xchange.binance.futures.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum OrderType {
    LIMIT,
    MARKET,
    STOP,
    STOP_MARKET,
    TAKE_PROFIT,
    TAKE_PROFIT_MARKET,
    TRAILING_STOP_MARKET;

    @JsonCreator
    public static OrderType getOrderType(String s) {
        try {
            return OrderType.valueOf(s);
        } catch (Exception e) {
            throw new RuntimeException("Unknown order type " + s + ".");
        }
    }
}
