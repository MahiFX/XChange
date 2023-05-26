package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.TradeService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;

public class VertexStreamingExchange extends BaseExchange implements StreamingExchange {

    private static final String WS_TESTNET_API_URL = "wss://test.vertexprotocol-backend.com";
    private static final String WS_API_URL = "wss://prod.vertexprotocol-backend.com";
    private VertexStreamingService marketDataStreamService;
    private VertexStreamingMarketDataService streamingMarketDataService;
    private VertexStreamingTradeService streamingTradeService;

    private boolean useTestnet;

    private String chainId;

    private String endPointContract;

    private String bookContract;

    private VertexStreamingService orderStreamService;
    private VertexProductInfo productInfo;


    private VertexStreamingService createStreamingService(String suffix) {
        VertexStreamingService streamingService = new VertexStreamingService(getApiUrl() + suffix);
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
    public void applyStreamingSpecification(ExchangeSpecification exchangeSpec, NettyStreamingService<?> streamingService) {
        StreamingExchange.super.applyStreamingSpecification(exchangeSpec, streamingService);
        this.useTestnet = !Boolean.FALSE.equals(exchangeSpec.getExchangeSpecificParametersItem(USE_SANDBOX));
    }

    @Override
    public void remoteInit() throws ExchangeException {


        orderStreamService.connect().blockingAwait();

        CountDownLatch responseLatch = new CountDownLatch(1);

        Disposable subscription = orderStreamService.subscribeChannel(ALL_MESSAGES)
                .subscribe(resp -> {
                    JsonNode data = resp.get("data");
                    if (data != null) {
                        chainId = data.get("chain_id").asText();
                        endPointContract = data.get("endpoint_addr").asText();
                        bookContract = data.withArray("book_addrs").get(1).asText();
                    }
                    responseLatch.countDown();
                });
        try {
            orderStreamService.sendMessage("{\"type\":\"contracts\"}");
            try {
                responseLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Failed to get contract data due to timeout");
            }
            if (chainId == null) {
                throw new IllegalStateException("Failed to load Vertex exchange details");
            }
        } finally {
            subscription.dispose();
        }


    }

    @Override
    protected void initServices() {

        productInfo = new VertexProductInfo();

        this.marketDataStreamService = createStreamingService("/subscribe");
        this.orderStreamService = createStreamingService("/ws");

    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        if (this.streamingMarketDataService == null) {
            this.streamingMarketDataService = new VertexStreamingMarketDataService(marketDataStreamService, productInfo);
        }
        return streamingMarketDataService;
    }

    @Override
    public VertexStreamingTradeService getStreamingTradeService() {
        if (this.streamingTradeService == null) {
            this.streamingTradeService = new VertexStreamingTradeService(orderStreamService, getExchangeSpecification(), productInfo, chainId, bookContract);
        }
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
