package org.knowm.xchange.binance.futures.service;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;

import static org.knowm.xchange.binance.BinanceResilience.REQUEST_WEIGHT_RATE_LIMITER;

public class BinanceFuturesMarketDataServiceRaw extends BinanceFuturesBaseService {
    protected BinanceFuturesMarketDataServiceRaw(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, binanceFutures, resilienceRegistries);
    }

    public BinanceOrderbook getBinanceOrderbook(CurrencyPair pair, Integer limit) throws IOException {
        return decorateApiCall(() -> binanceFutures.depth(BinanceAdapters.toSymbol(pair), limit))
                .withRetry(retry("depth"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER), depthPermits(limit))
                .call();
    }

    protected int depthPermits(Integer limit) {
        if (limit == null || limit <= 100) {
            return 1;
        } else if (limit <= 500) {
            return 5;
        } else if (limit <= 1000) {
            return 10;
        }
        return 50;
    }
}
