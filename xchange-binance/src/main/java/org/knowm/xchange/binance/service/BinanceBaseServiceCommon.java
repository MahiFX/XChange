package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceCommon;
import org.knowm.xchange.binance.BinanceExchangeCommon;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.service.BaseResilientExchangeService;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;

import static org.knowm.xchange.binance.BinanceResilience.REQUEST_WEIGHT_RATE_LIMITER;

public class BinanceBaseServiceCommon<T extends BinanceExchangeCommon> extends BaseResilientExchangeService<T> {
    private final BinanceCommon binance;

    public BinanceBaseServiceCommon(T exchange, ResilienceRegistries resilienceRegistries, BinanceCommon binance) {
        super(exchange, resilienceRegistries);
        this.binance = binance;
    }

    public Long getRecvWindow() {
        return (Long)
                exchange.getExchangeSpecification().getExchangeSpecificParametersItem("recvWindow");
    }

    public SynchronizedValueFactory<Long> getTimestampFactory() {
        return exchange.getTimestampFactory();
    }

    public BinanceExchangeInfo getExchangeInfo() throws IOException {
        return decorateApiCall(binance::exchangeInfo)
                .withRetry(retry("exchangeInfo"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }
}
