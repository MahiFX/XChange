package org.knowm.xchange.binance;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceFuturesAccountService;
import org.knowm.xchange.binance.service.BinanceFuturesTradeService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.utils.AuthUtils;

public class BinanceFuturesExchange extends BinanceExchange {
    @Override
    protected BinanceAuthenticated binance() {
        return ExchangeRestProxyBuilder.forInterface(
                BinanceFuturesAuthenticated.class, getExchangeSpecification())
                .build();
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

    @Override
    protected void binanceTradeService(BinanceAuthenticated binance) {
        assert binance instanceof BinanceFuturesAuthenticated;
        this.tradeService = new BinanceFuturesTradeService(this, (BinanceFuturesAuthenticated) binance, getResilienceRegistries());
    }

    @Override
    protected BinanceAccountService binanceAccountService(BinanceAuthenticated binance) {
        return new BinanceFuturesAccountService(this, binance, getResilienceRegistries());
    }
}
