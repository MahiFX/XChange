package com.knowm.xchange.vertex;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.knowm.xchange.vertex.dto.Symbol;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;

import java.math.BigDecimal;
import java.util.*;

public class VertexProductInfo {


  private final Set<Long> spotProducts;

  private final BiMap<Long, Instrument> productIdToInstrument = HashBiMap.create();

  private final Map<Long, BigDecimal> takerFees = new HashMap<>();

  private final Map<Long, BigDecimal> makerFees = new HashMap<>();

  private final BigDecimal takerSequencerFee;
  private final BigDecimal interestFee;

  public VertexProductInfo(Set<Long> spotProducts, Symbol[] symbols, List<BigDecimal> takerFeeList, List<BigDecimal> makerFeeList, BigDecimal takerSequencerFee, BigDecimal interestFee) {
    this.spotProducts = spotProducts;
    this.takerSequencerFee = takerSequencerFee;
    this.interestFee = interestFee;
    for (Symbol symbol : symbols) {
      long productId = symbol.getProduct_id();
      CurrencyPair usdcPair = new CurrencyPair(symbol.getSymbol(), "USDC");
      productIdToInstrument.put(productId, usdcPair);
    }


    productIdToInstrument.put(0L, new CurrencyPair("USDC", "USDC"));

    for (int i = 0; i < takerFeeList.size(); i++) {
      BigDecimal value = takerFeeList.get(i);
      if (value.compareTo(BigDecimal.ZERO) < 0) {
        value = value.negate();
      }
      takerFees.put((long) i, value);
    }
    for (int i = 0; i < makerFeeList.size(); i++) {
      BigDecimal value = makerFeeList.get(i);
      if (value.compareTo(BigDecimal.ZERO) < 0) {
        value = value.negate();
      }
      makerFees.put((long) i, value);
    }
  }

  public long lookupProductId(Instrument currencyPair) {
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

  public BigDecimal makerTradeFee(long productId) {
    return makerFees.get(productId);
  }

  public BigDecimal takerTradeFee(long productId) {
    return takerFees.get(productId);
  }

  public BigDecimal takerSequencerFee() {
    return takerSequencerFee;
  }

  public BigDecimal interestFee() {
    return interestFee;
  }
}
