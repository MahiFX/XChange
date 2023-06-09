package com.knowm.xchange.vertex;

import org.knowm.xchange.instrument.Instrument;

import java.util.List;
import java.util.Set;

public class VertexProductInfo {


    private final Set<Long> spotProducts;
    private final Set<Long> perpProducts;

    public VertexProductInfo(Set<Long> spotProducts, Set<Long> perpProducts) {

        this.spotProducts = spotProducts;
        this.perpProducts = perpProducts;
    }

    long lookupProductId(Instrument currencyPair) {
        switch (currencyPair.toString().toUpperCase()) {
            case "WBTC/USDC":
            case "WBTC-USDC":
                return 1;
            case "BTC/PERP":
            case "BTC-PERP":
                return 2;
            case "WETH/USDC":
            case "WETH-USDC":
                return 3;
            case "ETH/PERP":
            case "ETH-PERP":
                return 4;
            default:
                throw new RuntimeException("unknown product id for " + currencyPair);
        }
    }

    public List<Long> getProductsIds() {
        return List.of(1L, 2L, 3L, 4L);
    }

    public boolean isSpot(Instrument instrument) {
        return spotProducts.contains(lookupProductId(instrument));
    }
}
