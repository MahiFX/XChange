package org.knowm.xchange.binance.futures;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.BinanceTimestampFactory;
import org.knowm.xchange.binance.futures.service.BinanceFuturesMarketDataService;
import org.knowm.xchange.binance.futures.service.BinanceFuturesTradeService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.utils.AuthUtils;

public class BinanceFuturesExchange extends BinanceExchange {
    public static final String DEFAULT_SSL_URL = "https://fapi.binance.com/fapi";
    public static final String TESTNET_SSL_URL = "https://testnet.binancefuture.com/fapi";
    private org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated binance;

    @Override
    protected void initServices() {
        this.binance = ExchangeRestProxyBuilder.forInterface(
                BinanceFuturesAuthenticated.class, getExchangeSpecification())
            .build();
        this.timestampFactory =
            new BinanceTimestampFactory(getExchangeSpecification().getResilience(), getResilienceRegistries());
        this.marketDataService = new BinanceFuturesMarketDataService(this, binance, getResilienceRegistries());
        this.tradeService = new BinanceFuturesTradeService(this, binance, getResilienceRegistries());
//        this.accountService = new BinanceFuturesAccountService(this, binance, getResilienceRegistries()); TODO - Binance Futures
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification spec = new ExchangeSpecification(this.getClass());
        spec.setSslUri(DEFAULT_SSL_URL);
        spec.setHost("www.binance.com");
        spec.setPort(80);
        spec.setExchangeName("Binance Futures");
        spec.setExchangeDescription("Binance USD-M Futures Exchange.");
        AuthUtils.setApiAndSecretKey(spec, "binance_futures");
        return spec;
    }

    @Override
    public void applySpecification(ExchangeSpecification exchangeSpecification) {
        concludeHostParams(exchangeSpecification);
        super.applySpecification(exchangeSpecification);
    }

    /**
     * Adjust host parameters depending on exchange specific parameters
     */
    protected void concludeHostParams(ExchangeSpecification exchangeSpecification) {
        if (exchangeSpecification.getExchangeSpecificParameters() != null) {
            if (enabledSandbox(exchangeSpecification)) {
                exchangeSpecification.setSslUri(TESTNET_SSL_URL);
                exchangeSpecification.setHost("testnet.binancefuture.com");
            }
        }
    }

    @Override
    public void remoteInit() {
        try {
            super.remoteInit();
        } catch (Throwable ignored) {
        }
    }
}
