package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.client.ResilienceRegistries;

public class BinanceFuturesBaseService extends BinanceBaseService {
    protected BinanceFuturesExchange exchange;
    protected BinanceFuturesAuthenticated binanceFutures;

    protected BinanceFuturesBaseService(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, binanceFutures, resilienceRegistries);

        this.exchange = exchange;
        this.binanceFutures = binanceFutures;
    }
}
