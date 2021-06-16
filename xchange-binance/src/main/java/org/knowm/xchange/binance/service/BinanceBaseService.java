package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.client.ResilienceRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.mazi.rescu.ParamsDigest;

public class BinanceBaseService extends BinanceBaseServiceCommon<BinanceExchange> {

  protected final Logger LOG = LoggerFactory.getLogger(getClass());

  protected final String apiKey;
  protected final BinanceAuthenticated binance;
  protected final ParamsDigest signatureCreator;

  protected BinanceBaseService(
          BinanceExchange exchange,
          BinanceAuthenticated binance,
          ResilienceRegistries resilienceRegistries) {

    super(exchange, resilienceRegistries, binance);
    this.binance = binance;
    this.apiKey = exchange.getExchangeSpecification().getApiKey();
    this.signatureCreator =
            BinanceHmacDigest.createInstance(exchange.getExchangeSpecification().getSecretKey());
  }
}
