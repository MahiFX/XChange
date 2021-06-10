package org.knowm.xchange.binance;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.service.BinanceFuturesTradeService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.utils.AuthUtils;

public class BinanceFuturesExchange extends BinanceExchangeCommon {
    private BinanceFuturesAuthenticated binance;

    @Override
    protected void initServices() {
        this.binance = ExchangeRestProxyBuilder.forInterface(
                BinanceFuturesAuthenticated.class, getExchangeSpecification())
                .build();
        this.timestampFactory =
                new BinanceTimestampFactory(
                        binance, getExchangeSpecification().getResilience(), getResilienceRegistries());
//        this.marketDataService = new BinanceMarketDataService(this, binance, getResilienceRegistries()); TODO - Binance Futures
        this.tradeService = new BinanceFuturesTradeService(this, binance, getResilienceRegistries());
//        this.accountService = new BinanceFuturesAccountService(this, binance, getResilienceRegistries()); TODO - Binance Futures
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification spec = new ExchangeSpecification(this.getClass());
        spec.setSslUri("https://fapi.binance.com");
        spec.setHost("www.binance.com");
        spec.setPort(80);
        spec.setExchangeName("Binance Futures");
        spec.setExchangeDescription("Binance Futures Exchange.");
        AuthUtils.setApiAndSecretKey(spec, "binance_futures");
        return spec;
    }
}
