package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.client.ResilienceRegistries;
import si.mazi.rescu.ParamsDigest;

public class BinanceFuturesBaseService extends BinanceBaseServiceCommon<BinanceFuturesExchange> {
    protected BinanceFuturesAuthenticated binanceFutures;

    protected final String apiKey;
    protected final ParamsDigest signatureCreator;

    protected BinanceFuturesBaseService(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, resilienceRegistries, binanceFutures);
        this.binanceFutures = binanceFutures;

        this.apiKey = exchange.getExchangeSpecification().getApiKey();
        this.signatureCreator =
                BinanceHmacDigest.createInstance(exchange.getExchangeSpecification().getSecretKey());
    }
}
