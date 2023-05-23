package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;

import java.util.Collections;

public class BinanceFuturesStreamingService extends BinanceStreamingService {
    public BinanceFuturesStreamingService(String baseUri, ProductSubscription productSubscription) {
        super(baseUri, productSubscription, new KlineSubscription(Collections.emptyMap()));
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionHandler.INSTANCE;
    }
}
