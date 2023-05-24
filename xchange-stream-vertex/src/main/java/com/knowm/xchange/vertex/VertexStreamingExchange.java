package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.service.trade.TradeService;

public class VertexStreamingExchange extends BaseExchange implements StreamingExchange {

    private static final String WS_TESTNET_API_URL = "wss://test.vertexprotocol-backend.com";
    private static final String WS_API_URL = "wss://prod.vertexprotocol-backend.com";
    private VertexStreamingService marketDataStreamService;
    private VertexStreamingMarketDataService streamingMarketDataService;
    private VertexStreamingTradeService streamingTradeService;

    private boolean useTestnet;
    private VertexStreamingService orderStreamService;


    private VertexStreamingService createStreamingService(String suffix) {
        VertexStreamingService streamingService = new VertexStreamingService(getApiUrl() + suffix, getExchangeSpecification());
        applyStreamingSpecification(getExchangeSpecification(), streamingService);

        return streamingService;
    }

    private String getApiUrl() {
        if (useTestnet) {
            return WS_TESTNET_API_URL;
        } else {

            return WS_API_URL;

        }
    }


    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass());
        exchangeSpecification.setSslUri("https://prod.vertexprotocol-backend.com");
        exchangeSpecification.setHost("prod.vertexprotocol-backend.com");
        exchangeSpecification.setExchangeName("Vertex");
        exchangeSpecification.setExchangeDescription("Vertex - One DEX. Everything you need.");
        return exchangeSpecification;
    }

    @Override
    protected void initServices() {
        this.useTestnet = Boolean.TRUE.equals(getExchangeSpecification().getExchangeSpecificParametersItem(USE_SANDBOX));

        this.marketDataStreamService = createStreamingService("/subscribe");
        this.streamingMarketDataService = new VertexStreamingMarketDataService(marketDataStreamService, getExchangeSpecification());
        this.orderStreamService = createStreamingService("/ws");
        this.streamingTradeService = new VertexStreamingTradeService(orderStreamService, getExchangeSpecification());
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
    }

    @Override
    public StreamingTradeService getStreamingTradeService() {
        return streamingTradeService;
    }

    @Override
    public TradeService getTradeService() {
        return streamingTradeService;
    }

    @Override
    public Observable<ConnectionStateModel.State> connectionStateObservable() {
        return marketDataStreamService.subscribeConnectionState();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return marketDataStreamService.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return marketDataStreamService.subscribeConnectionSuccess();
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return marketDataStreamService.connect();
    }

    @Override
    public Completable disconnect() {
        return marketDataStreamService.disconnect();
    }

    @Override
    public boolean isAlive() {
        return marketDataStreamService.isSocketOpen();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        marketDataStreamService.useCompressedMessages(compressedMessages);
    }
}
