package info.bitrich.xchangestream.binance;

import com.google.common.base.MoreObjects;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import info.bitrich.xchangestream.util.Events;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.service.BinanceFuturesMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY;

public class BinanceFuturesStreamingExchange extends BinanceFuturesExchange implements StreamingExchange {
    private static final String WS_USD_FUTURES_API_BASE_URI = "wss://fstream.binance.com/";
    private static final String WS_USD_TESTNET_FUTURES_API_BASE_URI = "wss://stream.binancefuture.com/";

    private Runnable onApiCall;
    private String orderBookUpdateFrequencyParameter = "";
    private BinanceStreamingService streamingService;
    private BinanceFuturesStreamingMarketDataService streamingMarketDataService;

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

    private String streamingUri(ProductSubscription subscription) {
        String path = wsUri(exchangeSpecification);

        path += "stream?streams=" + buildSubscriptionStreams(subscription);
        return path;
    }

    private String wsUri(ExchangeSpecification exchangeSpecification) {
        boolean useSandbox = Boolean.TRUE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX));

        return useSandbox ? WS_USD_TESTNET_FUTURES_API_BASE_URI : WS_USD_FUTURES_API_BASE_URI;
    }

    private String buildSubscriptionStreams(ProductSubscription subscription) {
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

        streamingMarketDataService = new BinanceFuturesStreamingMarketDataService(
                streamingService,
                getBinanceOrderBookProvider(),
                onApiCall,
                orderBookUpdateFrequencyParameter);

        return Completable.concat(completables)
                .doOnComplete(() -> streamingMarketDataService.openSubscriptions(subscriptions));
    }

    private Function<CurrencyPair, BinanceOrderbook> getBinanceOrderBookProvider() {
        return currencyPair -> {
            try {
                return ((BinanceFuturesMarketDataService) marketDataService).getBinanceOrderbook(currencyPair, 1000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
    }

    private BinanceStreamingService createStreamingService(ProductSubscription subscription) {
        return new BinanceFuturesStreamingService(streamingUri(subscription), subscription);
    }

    @Override
    public Completable disconnect() {
        List<Completable> completables = new ArrayList<>();
        completables.add(streamingService.disconnect());
        streamingService = null;
        streamingMarketDataService = null;
        return Completable.concat(completables);
    }

    @Override
    public Observable<ConnectionStateModel.State> connectionStateObservable() {
        return streamingService.subscribeConnectionState();
    }

    @Override
    public boolean isAlive() {
        return streamingService != null && streamingService.isSocketOpen();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        streamingService.useCompressedMessages(compressedMessages);
    }
}
