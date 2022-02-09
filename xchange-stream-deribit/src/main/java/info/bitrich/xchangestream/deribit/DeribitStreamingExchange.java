package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.deribit.v2.DeribitExchange;
import org.knowm.xchange.service.trade.TradeService;

public class DeribitStreamingExchange extends DeribitExchange implements StreamingExchange {
    public static final String MAX_DEPTH_MD = "deribitMaxDepthMD";

    private static final String WS_API_URL = "wss://www.deribit.com/ws/api/v2";
    private static final String WS_GATEWAY_API_URL = "ws://gateway.deribit.com:8022/ws/api/v2";
    private static final String WS_TESTNET_API_URL = "wss://test.deribit.com/ws/api/v2";

    private boolean useTestnet;

    private DeribitStreamingService streamingService;
    private DeribitStreamingMarketDataService streamingMarketDataService;
    private DeribitStreamingTradeService streamingTradeService;

    @Override
    protected void initServices() {
        super.initServices();
        this.useTestnet = Boolean.TRUE.equals(getExchangeSpecification().getExchangeSpecificParametersItem(USE_SANDBOX));

        this.streamingService = createStreamingService();
        this.streamingMarketDataService = new DeribitStreamingMarketDataService(streamingService, getExchangeSpecification());
        this.streamingTradeService = new DeribitStreamingTradeService(streamingService, getExchangeSpecification());
    }

    private DeribitStreamingService createStreamingService() {
        DeribitStreamingService streamingService = new DeribitStreamingService(getApiUrl(), getExchangeSpecification());
        applyStreamingSpecification(getExchangeSpecification(), streamingService);

        return streamingService;
    }

    private String getApiUrl() {
        if (useTestnet) {
            return WS_TESTNET_API_URL;
        } else {
            if (getExchangeSpecification().getSslUri().contains("gateway.deribit.com")) {
                return WS_GATEWAY_API_URL;
            } else {
                return WS_API_URL;
            }
        }
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
        return streamingService.subscribeConnectionState();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return streamingService.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return streamingService.subscribeConnectionSuccess();
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return streamingService.connect();
    }

    @Override
    public Completable disconnect() {
        return streamingService.disconnect();
    }

    @Override
    public boolean isAlive() {
        return streamingService.isSocketOpen();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        streamingService.useCompressedMessages(compressedMessages);
    }
}
