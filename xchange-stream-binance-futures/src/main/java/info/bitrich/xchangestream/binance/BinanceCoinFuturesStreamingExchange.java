package info.bitrich.xchangestream.binance;

import org.knowm.xchange.ExchangeSpecification;

public class BinanceCoinFuturesStreamingExchange extends BinanceFuturesStreamingExchange {
    private static final String WS_COIN_FUTURES_API_BASE_URI = "wss://dstream.binance.com/";
    private static final String WS_COIN_TESTNET_FUTURES_API_BASE_URI = "wss://dstream.binancefuture.com/";

    @Override
    protected String wsUri(ExchangeSpecification exchangeSpecification) {
        if (exchangeSpecification.getOverrideWebsocketApiUri() != null) {
            return exchangeSpecification.getOverrideWebsocketApiUri();
        }

        boolean useSandbox = Boolean.TRUE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX));

        if (useSandbox) {
            return WS_COIN_TESTNET_FUTURES_API_BASE_URI;
        } else {
            return WS_COIN_FUTURES_API_BASE_URI;
        }
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification spec = super.getDefaultExchangeSpecification();

        spec.setSslUri("https://dapi.binance.com/dapi");
        spec.setExchangeDescription("Binance COIN-M Futures Exchange.");

        return spec;
    }
}
