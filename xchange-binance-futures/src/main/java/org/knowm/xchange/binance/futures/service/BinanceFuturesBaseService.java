package org.knowm.xchange.binance.futures.service;

import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.binance.service.BinanceHmacDigest;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.service.BaseResilientExchangeService;
import si.mazi.rescu.ParamsDigest;

import java.io.IOException;

import static org.knowm.xchange.binance.BinanceResilience.REQUEST_WEIGHT_RATE_LIMITER;

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


    public BinanceExchangeInfo getExchangeInfo() throws IOException {
        return decorateApiCall(binanceFutures::exchangeInfo)
                .withRetry(retry("exchangeInfo"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }
}
