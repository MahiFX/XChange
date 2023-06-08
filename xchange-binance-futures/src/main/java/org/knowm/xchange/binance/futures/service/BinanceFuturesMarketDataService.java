package org.knowm.xchange.binance.futures.service;

import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.service.marketdata.MarketDataService;

public class BinanceFuturesMarketDataService extends BinanceFuturesMarketDataServiceRaw implements MarketDataService {
    public BinanceFuturesMarketDataService(BinanceFuturesExchange exchange, BinanceFuturesAuthenticated binanceFutures, ResilienceRegistries resilienceRegistries) {
        super(exchange, binanceFutures, resilienceRegistries);
    }

}
