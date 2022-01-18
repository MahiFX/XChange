package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.deribit.v2.DeribitExchange;

public class DeribitStreamingExchange extends DeribitExchange implements StreamingExchange {
    private static final String WS_API_URL = "wss://www.deribit.com/ws/api/v2";
    private static final String WS_TESTNET_API_URL = "wss://test.deribit.com/ws/api/v2";

    private boolean useTestnet;

    private DeribitStreamingService streamingService;
    private DeribitStreamingMarketDataService streamingMarketDataService;

    @Override
    protected void initServices() {
        super.initServices();
        this.useTestnet = Boolean.TRUE.equals(getExchangeSpecification().getExchangeSpecificParametersItem(USE_SANDBOX));

        this.streamingService = createStreamingService();
        this.streamingMarketDataService = new DeribitStreamingMarketDataService(streamingService, getExchangeSpecification());
    }

    private DeribitStreamingService createStreamingService() {
        DeribitStreamingService streamingService = new DeribitStreamingService(useTestnet ? WS_TESTNET_API_URL : WS_API_URL, getExchangeSpecification());
        applyStreamingSpecification(getExchangeSpecification(), streamingService);

        return streamingService;
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
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
