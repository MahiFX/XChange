package info.bitrich.xchangestream.binance;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

public class BinanceFuturesUserDataStreamingService extends BinanceUserDataStreamingService {
    private static final String FUTURES_API_BASE_URI = "wss://fstream.binance.com/ws/";
    private static final String FUTURES_TESTNET_URI = "wss://stream.binancefuture.com/ws/";

    public static BinanceFuturesUserDataStreamingService create(String listenKey, boolean useSandbox) {
        return new BinanceFuturesUserDataStreamingService(baseUri(useSandbox) + listenKey);
    }

    private BinanceFuturesUserDataStreamingService(String url) {
        super(url);
    }

    private static String baseUri(boolean useSandbox) {
        return useSandbox ? FUTURES_TESTNET_URI : FUTURES_API_BASE_URI;
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionHandler.INSTANCE;
    }
}
