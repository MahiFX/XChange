package com.knowm.xchange.vertex;

import org.knowm.xchange.instrument.Instrument;

import java.util.List;

public class VertexProductInfo {
    long lookupProductId(Instrument currencyPair) {
        switch (currencyPair.toString().toUpperCase()) {
            case "WBTC/USDC":
                return 1;
            case "BTC-PERP":
                return 2;
            case "WETH/USDC":
                return 3;
            case "ETH-PERP":
                return 4;
            default:
                throw new RuntimeException("unknown product id for " + currencyPair);
        }
    }

    public List<Long> getProductsIds() {
        return List.of(1L, 2L, 3L, 4L);
    }
}
