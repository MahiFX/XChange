package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.Completable;
import org.knowm.xchange.binance.BinanceFuturesExchange;

public class BinanceFuturesStreamingExchange extends BinanceFuturesExchange implements StreamingExchange {
    private static final String WS_USD_FUTURES_API_BASE_URI = "wss://fstream.binance.com/";

    // TODO - Binance Futures


//    @Override
//    protected String wsUri(ExchangeSpecification exchangeSpecification) {
//        return WS_USD_FUTURES_API_BASE_URI;
//    }
//
//    @Override
//    protected BinanceAccountService binanceAccountService(BinanceAuthenticated binance) {
//        return new BinanceFuturesAccountService(this, binance, getResilienceRegistries());
//    }
//
//    @Override
//    protected BinanceStreamingService createStreamingService(ProductSubscription subscription) {
//        return new BinanceFuturesStreamingService(streamingUri(subscription), subscription);
//    }
//
//    @Override
//    protected BinanceStreamingMarketDataService streamingMarketDataService(BinanceStreamingService streamingService, BinanceMarketDataService marketDataService, Runnable onApiCall, String orderBookUpdateFrequencyParameter) {
//        return new BinanceFuturesStreamingMarketDataService(streamingService, marketDataService, onApiCall, orderBookUpdateFrequencyParameter);
//    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return null;
    }

    @Override
    public Completable disconnect() {
        return null;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {

    }
}
