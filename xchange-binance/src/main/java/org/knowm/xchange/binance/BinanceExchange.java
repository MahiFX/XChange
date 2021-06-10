package org.knowm.xchange.binance;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.utils.AuthUtils;

public class BinanceExchange extends BinanceExchangeCommon {
    private BinanceAuthenticated binance;

    @Override
    protected void initServices() {
        this.binance = ExchangeRestProxyBuilder.forInterface(
                BinanceAuthenticated.class, getExchangeSpecification())
                .build();
        this.timestampFactory =
                new BinanceTimestampFactory(
                        binance, getExchangeSpecification().getResilience(), getResilienceRegistries());
        this.marketDataService = new BinanceMarketDataService(this, binance, getResilienceRegistries());
        this.tradeService = new BinanceTradeService(this, binance, getResilienceRegistries());
        this.accountService = new BinanceAccountService(this, binance, getResilienceRegistries());
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {

        ExchangeSpecification spec = new ExchangeSpecification(this.getClass());
        spec.setSslUri("https://api.binance.com");
        spec.setHost("www.binance.com");
        spec.setPort(80);
        spec.setExchangeName("Binance");
        spec.setExchangeDescription("Binance Exchange.");
        AuthUtils.setApiAndSecretKey(spec, "binance");
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
                    exchangeSpecification.getExchangeSpecificParametersItem("Use_Sandbox"))) {
                exchangeSpecification.setSslUri("https://testnet.binance.vision");
                exchangeSpecification.setHost("testnet.binance.vision");
            }
        }
    }

}
