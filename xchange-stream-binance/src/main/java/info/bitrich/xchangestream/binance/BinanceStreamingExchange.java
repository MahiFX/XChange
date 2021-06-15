package info.bitrich.xchangestream.binance;

import com.google.common.base.MoreObjects;
import info.bitrich.xchangestream.binance.BinanceUserDataChannel.NoActiveChannelException;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel.State;
import info.bitrich.xchangestream.util.Events;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.currency.CurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BinanceStreamingExchange extends BinanceExchange implements StreamingExchange {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceStreamingExchange.class);
    private static final String WS_API_BASE_URI = "wss://stream.binance.com:9443/";
    private static final String WS_SANDBOX_API_BASE_URI = "wss://testnet.binance.vision/";
    protected static final String USE_HIGHER_UPDATE_FREQUENCY =
            "Binance_Orderbook_Use_Higher_Frequency";

    private BinanceStreamingService streamingService;
    private BinanceUserDataStreamingService userDataStreamingService;

    private BinanceStreamingMarketDataService streamingMarketDataService;
    private BinanceStreamingAccountService streamingAccountService;
    private BinanceStreamingTradeService streamingTradeService;

    private BinanceUserDataChannel userDataChannel;
    private Runnable onApiCall;
    private String orderBookUpdateFrequencyParameter = "";

    @Override
    protected void initServices() {
        super.initServices();
        this.onApiCall = Events.onApiCall(exchangeSpecification);
        Boolean userHigherFrequency =
                MoreObjects.firstNonNull(
                        (Boolean)
                                exchangeSpecification.getExchangeSpecificParametersItem(
                                        USE_HIGHER_UPDATE_FREQUENCY),
                        Boolean.FALSE);

        if (userHigherFrequency) {
            orderBookUpdateFrequencyParameter = "@100ms";
        }
    }

    /**
     * Binance streaming API expects connections to multiple channels to be defined at connection
     * time. To define the channels for this connection pass a `ProductSubscription` in at connection
     * time.
     *
     * @param args A single `ProductSubscription` to define the subscriptions required to be available
     *             during this connection.
     * @return A completable which fulfils once connection is complete.
     */
    @Override
    public Completable connect(ProductSubscription... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Subscriptions must be made at connection time");
        }
        if (streamingService != null) {
            throw new UnsupportedOperationException(
                    "Exchange only handles a single connection - disconnect the current connection.");
        }

        ProductSubscription subscriptions = args[0];
        streamingService = createStreamingService(subscriptions);

        List<Completable> completables = new ArrayList<>();

        if (subscriptions.hasUnauthenticated()) {
            completables.add(streamingService.connect());
        }

        if (subscriptions.hasAuthenticated()) {
            if (exchangeSpecification.getApiKey() == null) {
                throw new IllegalArgumentException("API key required for authenticated streams");
            }

            LOG.info("Connecting to authenticated web socket");
            BinanceAuthenticated binance =
                    ExchangeRestProxyBuilder.forInterface(
                            BinanceAuthenticated.class, getExchangeSpecification())
                            .build();
            userDataChannel =
                    new BinanceUserDataChannel(binance, exchangeSpecification.getApiKey(), onApiCall);
            try {
                completables.add(createAndConnectUserDataService(userDataChannel.getListenKey()));
            } catch (NoActiveChannelException e) {
                throw new IllegalStateException("Failed to establish user data channel", e);
            }
        }

        streamingMarketDataService = streamingMarketDataService(
                streamingService,
                (BinanceMarketDataService) marketDataService,
                onApiCall,
                orderBookUpdateFrequencyParameter);
        streamingAccountService = new BinanceStreamingAccountService(userDataStreamingService);
        streamingTradeService = new BinanceStreamingTradeService(userDataStreamingService);

        return Completable.concat(completables)
                .doOnComplete(() -> streamingMarketDataService.openSubscriptions(subscriptions))
                .doOnComplete(() -> streamingAccountService.openSubscriptions())
                .doOnComplete(() -> streamingTradeService.openSubscriptions());
    }

    private BinanceStreamingMarketDataService streamingMarketDataService(BinanceStreamingService streamingService, BinanceMarketDataService marketDataService, Runnable onApiCall, String orderBookUpdateFrequencyParameter) {
        return new BinanceStreamingMarketDataService(
                streamingService,
                currencyPair -> {
                    try {
                        return marketDataService.getBinanceOrderbook(currencyPair, 1000);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                onApiCall,
                orderBookUpdateFrequencyParameter
        );
    }

    private Completable createAndConnectUserDataService(String listenKey) {
        userDataStreamingService = BinanceUserDataStreamingService.create(listenKey, Boolean.TRUE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX)));
        return userDataStreamingService
                .connect()
                .doOnComplete(
                        () -> {
                            LOG.info("Connected to authenticated web socket");
                            userDataChannel.onChangeListenKey(
                                    newListenKey -> {
                                        userDataStreamingService
                                                .disconnect()
                                                .doOnComplete(
                                                        () -> {
                                                            createAndConnectUserDataService(newListenKey)
                                                                    .doOnComplete(
                                                                            () -> {
                                                                                streamingAccountService.setUserDataStreamingService(
                                                                                        userDataStreamingService);
                                                                                streamingTradeService.setUserDataStreamingService(
                                                                                        userDataStreamingService);
                                                                            });
                                                        });
                                    });
                        });
    }

    @Override
    public Completable disconnect() {
        List<Completable> completables = new ArrayList<>();
        completables.add(streamingService.disconnect());
        streamingService = null;
        if (userDataStreamingService != null) {
            completables.add(userDataStreamingService.disconnect());
            userDataStreamingService = null;
        }
        if (userDataChannel != null) {
            userDataChannel.close();
            userDataChannel = null;
        }
        streamingMarketDataService = null;
        return Completable.concat(completables);
    }

    @Override
    public boolean isAlive() {
        return (streamingService != null && streamingService.isSocketOpen() || userDataStreamingService != null && userDataStreamingService.isSocketOpen());
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
    public Observable<State> connectionStateObservable() {
        return streamingService.subscribeConnectionState();
    }

    @Override
    public BinanceStreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
    }

    @Override
    public BinanceStreamingAccountService getStreamingAccountService() {
        return streamingAccountService;
    }

    @Override
    public BinanceStreamingTradeService getStreamingTradeService() {
        return streamingTradeService;
    }

    private BinanceStreamingService createStreamingService(ProductSubscription subscription) {
        return new BinanceStreamingService(streamingUri(subscription), subscription);
    }

    private String streamingUri(ProductSubscription subscription) {
        String path = wsUri(exchangeSpecification);

        path += "stream?streams=" + buildSubscriptionStreams(subscription);
        return path;
    }

    private String wsUri(ExchangeSpecification exchangeSpecification) {
        boolean useSandbox = Boolean.TRUE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX));

        return useSandbox ? WS_SANDBOX_API_BASE_URI : WS_API_BASE_URI;
    }

    public String buildSubscriptionStreams(ProductSubscription subscription) {
        return Stream.of(
                buildSubscriptionStrings(
                        subscription.getTicker(), BinanceSubscriptionType.TICKER.getType()),
                buildSubscriptionStrings(
                        subscription.getOrderBook(), BinanceSubscriptionType.DEPTH.getType()),
                buildSubscriptionStrings(
                        subscription.getTrades(), BinanceSubscriptionType.TRADE.getType()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }

    private String buildSubscriptionStrings(
            List<CurrencyPair> currencyPairs, String subscriptionType) {
        if (BinanceSubscriptionType.DEPTH.getType().equals(subscriptionType)) {
            return subscriptionStrings(currencyPairs)
                    .map(s -> s + "@" + subscriptionType + orderBookUpdateFrequencyParameter)
                    .collect(Collectors.joining("/"));
        } else {
            return subscriptionStrings(currencyPairs)
                    .map(s -> s + "@" + subscriptionType)
                    .collect(Collectors.joining("/"));
        }
    }

    private static Stream<String> subscriptionStrings(List<CurrencyPair> currencyPairs) {
        return currencyPairs.stream()
                .map(pair -> String.join("", pair.toString().split("/")).toLowerCase());
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        streamingService.useCompressedMessages(compressedMessages);
    }

    public void enableLiveSubscription() {
        if (this.streamingService == null) {
            throw new UnsupportedOperationException(
                    "You must connect to streams before enabling live subscription.");
        }
        this.streamingService.enableLiveSubscription();
    }

    public void disableLiveSubscription() {
        if (this.streamingService != null) this.streamingService.disableLiveSubscription();
    }
}
