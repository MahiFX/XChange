package info.bitrich.xchangestream.deribit;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
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
        this.streamingMarketDataService = new DeribitStreamingMarketDataService(streamingService);
    }

    private DeribitStreamingService createStreamingService() {
        DeribitStreamingService streamingService = new DeribitStreamingService(useTestnet ? WS_TESTNET_API_URL : WS_API_URL);
        applyStreamingSpecification(getExchangeSpecification(), streamingService);

        return streamingService;
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
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
