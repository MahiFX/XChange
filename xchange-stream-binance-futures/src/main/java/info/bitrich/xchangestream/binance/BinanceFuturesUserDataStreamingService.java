package info.bitrich.xchangestream.binance;

import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

public class BinanceFuturesUserDataStreamingService extends BinanceUserDataStreamingService {
    public static BinanceFuturesUserDataStreamingService create(String wsUri, String listenKey) {
        return new BinanceFuturesUserDataStreamingService(wsUri + "ws/" + listenKey);
    }

    private BinanceFuturesUserDataStreamingService(String url) {
        super(url);
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionHandler.INSTANCE;
    }
}
