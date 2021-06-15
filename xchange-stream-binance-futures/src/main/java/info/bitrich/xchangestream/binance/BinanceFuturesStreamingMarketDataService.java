package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.binance.dto.DepthBinanceWebSocketTransaction;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.function.Function;

public class BinanceFuturesStreamingMarketDataService extends BinanceStreamingMarketDataService {
    public BinanceFuturesStreamingMarketDataService(BinanceStreamingService service, Function<CurrencyPair, BinanceOrderbook> binanceOrderBookProvider, Runnable onApiCall, String orderBookUpdateFrequencyParameter) {
        super(service, binanceOrderBookProvider, onApiCall, orderBookUpdateFrequencyParameter);
    }

    @Override
    protected boolean checkDepthDataInOrder(DepthBinanceWebSocketTransaction depth, long lastUpdateId) {
        return lastUpdateId == depth.getLastUpdateIdFromPreviousEvent();
    }
}
