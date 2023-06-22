package com.knowm.xchange.vertex;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.knowm.xchange.vertex.dto.Symbol;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VertexProductInfo {


    private final Set<Long> spotProducts;

    BiMap<Long, Instrument> productIdToInstrument = HashBiMap.create();

    public VertexProductInfo(Set<Long> spotProducts, Symbol[] symbols) {
        this.spotProducts = spotProducts;
        for (Symbol symbol : symbols) {
            long productId = symbol.getProduct_id();
            CurrencyPair usdcPair = new CurrencyPair(symbol.getSymbol(), "USDC");
            productIdToInstrument.put(productId, usdcPair);
        }
    }

    long lookupProductId(Instrument currencyPair) {
        Long id = productIdToInstrument.inverse().get(currencyPair);
        if (id != null) {
            return id;
        }
        throw new RuntimeException("unknown product id for " + currencyPair);

    }

    public List<Long> getProductsIds() {
        return new ArrayList<>(productIdToInstrument.keySet());
    }

    public boolean isSpot(Instrument instrument) {
        return spotProducts.contains(lookupProductId(instrument));
    }

    public Instrument lookupInstrument(long productId) {
        return productIdToInstrument.get(productId);
    }
}
