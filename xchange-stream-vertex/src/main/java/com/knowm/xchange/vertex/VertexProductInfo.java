package com.knowm.xchange.vertex;

import org.knowm.xchange.currency.CurrencyPair;

public class VertexProductInfo {
    long lookupProductId(CurrencyPair currencyPair) {
        switch (currencyPair.toString()) {
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
}
