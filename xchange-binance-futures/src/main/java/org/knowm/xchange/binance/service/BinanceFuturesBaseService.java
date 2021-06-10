package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.service.BaseResilientExchangeService;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

public class BinanceFuturesBaseService extends BaseResilientExchangeService<BinanceFuturesExchange> {
    protected BinanceFuturesAuthenticated binanceFutures;

    protected final String apiKey;
    protected final ParamsDigest signatureCreator;

    protected BinanceFuturesBaseService(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, resilienceRegistries);
        this.binanceFutures = binanceFutures;

        this.apiKey = exchange.getExchangeSpecification().getApiKey();
        this.signatureCreator =
                BinanceHmacDigest.createInstance(exchange.getExchangeSpecification().getSecretKey());
    }

    public Long getRecvWindow() {
        return (Long)
                exchange.getExchangeSpecification().getExchangeSpecificParametersItem("recvWindow");
    }

    public SynchronizedValueFactory<Long> getTimestampFactory() {
        return exchange.getTimestampFactory();
    }
}
