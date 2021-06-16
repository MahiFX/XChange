package info.bitrich.xchangestream.cryptofacilities;

import com.google.common.base.MoreObjects;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel.State;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.cryptofacilities.CryptoFacilitiesExchange;

/**
 * @author makarid
 */
public class CryptoFacilitiesStreamingExchange extends CryptoFacilitiesExchange implements StreamingExchange {

    private static final String USE_BETA = "Use_Beta";
    private static final String KRAKEN_API_URI = "wss://futures.kraken.com/ws/v1";
    private static final String KRAKEN_API_BETA_URI = "wss://demo-futures.kraken.com/ws/v1";
    private static final String CF_API_URI = "wss://www.cryptofacilities.com/ws/v1";
    private static final String CF_API_BETA_URI = "wss://conformance.cryptofacilities.com/ws/v1";

    private info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingService streamingService, privateStreamingService;
    private info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingMarketDataService streamingMarketDataService;

    public CryptoFacilitiesStreamingExchange() {
    }

    public static String pickUri(ExchangeSpecification exchangeSpecification, boolean useBeta) {
        if (exchangeSpecification.getSslUri().contains("futures.kraken.com")) {
            return useBeta ? KRAKEN_API_BETA_URI : KRAKEN_API_URI;

        } else if (exchangeSpecification.getSslUri().contains("cryptofacilities.com")) {
            return useBeta ? CF_API_BETA_URI : CF_API_URI;

        } else {
            throw new IllegalArgumentException("Unsupported URL " + exchangeSpecification.getSslUri());

        }
    }

    @Override
    protected void initServices() {
        super.initServices();
        Boolean useBeta =
                MoreObjects.firstNonNull(
                        (Boolean) exchangeSpecification.getExchangeSpecificParametersItem(USE_BETA),
                        Boolean.FALSE);

        String uri = pickUri(exchangeSpecification, useBeta);

        this.streamingService =
                new info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingService(false, uri);
        this.streamingMarketDataService = new info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingMarketDataService(streamingService);

        if (StringUtils.isNotEmpty(exchangeSpecification.getApiKey())) {
            this.privateStreamingService =
                    new info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingService(true, uri);
        }
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        if (privateStreamingService != null)
            return privateStreamingService.connect().mergeWith(streamingService.connect());
        return streamingService.connect();
    }

    @Override
    public Completable disconnect() {
        if (privateStreamingService != null)
            return privateStreamingService.disconnect().mergeWith(streamingService.disconnect());
        return streamingService.disconnect();
    }

    @Override
    public boolean isAlive() {
        return streamingService.isSocketOpen()
                && (privateStreamingService == null || privateStreamingService.isSocketOpen());
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return streamingService.subscribeConnectionSuccess();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return streamingService.subscribeReconnectFailure();
    }

    @Override
    public Observable<State> connectionStateObservable() {
        return streamingService.subscribeConnectionState();
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification spec = super.getDefaultExchangeSpecification();
        spec.setSslUri("https://futures.kraken.com/derivatives");
        spec.setHost("futures.kraken.com/derivatives");
        spec.setExchangeName("Kraken Futures");
        spec.setShouldLoadRemoteMetaData(false);
        return spec;
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
    }

    @Override
    public StreamingTradeService getStreamingTradeService() {
//  Not yet implemented Trade side of Kraken Futures
        return null;
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        streamingService.useCompressedMessages(compressedMessages);
    }

    @Override
    public void resubscribeChannels() {
        logger.debug("Resubscribing channels");
        streamingService.resubscribeChannels();
        if (privateStreamingService != null)
            privateStreamingService.resubscribeChannels();
    }
}
