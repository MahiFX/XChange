package org.knowm.xchange.binance;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.service.BinanceFuturesMarketDataService;
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
        this.marketDataService = new BinanceFuturesMarketDataService(this, binance, getResilienceRegistries());
        this.tradeService = new BinanceFuturesTradeService(this, binance, getResilienceRegistries());
//        this.accountService = new BinanceFuturesAccountService(this, binance, getResilienceRegistries()); TODO - Binance Futures
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification spec = new ExchangeSpecification(this.getClass());
        spec.setSslUri("https://fapi.binance.com/fapi");
        spec.setHost("www.binance.com");
        spec.setPort(80);
        spec.setExchangeName("Binance Futures");
        spec.setExchangeDescription("Binance Futures Exchange.");
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
    private static void concludeHostParams(ExchangeSpecification exchangeSpecification) {
        if (exchangeSpecification.getExchangeSpecificParameters() != null) {
            if (Boolean.TRUE.equals(
                    exchangeSpecification.getExchangeSpecificParametersItem("Use_Sandbox")) && exchangeSpecification.getSslUri() == null) {
                exchangeSpecification.setSslUri("https://testnet.binancefuture.com");
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
