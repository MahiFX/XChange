package info.bitrich.xchangestream.binance;

import com.google.common.base.MoreObjects;
import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.FETCH_ORDER_BOOK_LIMIT;
import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY;
import static info.bitrich.xchangestream.binance.BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import info.bitrich.xchangestream.util.Events;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.ArrayList;
import static java.util.Collections.emptyMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.binance.futures.service.BinanceFuturesMarketDataService;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceFuturesStreamingExchange extends BinanceFuturesExchange implements StreamingExchange {
  private static final Logger LOG = LoggerFactory.getLogger(BinanceStreamingExchange.class);

  private static final String WS_USD_FUTURES_API_BASE_URI = "wss://fstream.binance.com/";
  private static final String WS_USD_TESTNET_FUTURES_API_BASE_URI = "wss://stream.binancefuture.com/";

  private Runnable onApiCall;
  private String orderBookUpdateFrequencyParameter = "";
  private boolean tickerRealtimeSubscriptionParameter = false;

  private BinanceStreamingService streamingService;
  private BinanceUserDataStreamingService userDataStreamingService;

  private BinanceFuturesStreamingMarketDataService streamingMarketDataService;
  private BinanceFuturesStreamingTradeService streamingTradeService;

  private BinanceUserDataChannel userDataChannel;
  private int oderBookFetchLimitParameter;

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

    tickerRealtimeSubscriptionParameter =
        MoreObjects.firstNonNull(
            (Boolean)
                exchangeSpecification.getExchangeSpecificParametersItem(
                    USE_REALTIME_BOOK_TICKER),
            Boolean.FALSE);

    Object fetchOrderBookLimit =
        exchangeSpecification.getExchangeSpecificParametersItem(FETCH_ORDER_BOOK_LIMIT);
    if (fetchOrderBookLimit instanceof Integer) {
      oderBookFetchLimitParameter = (int) fetchOrderBookLimit;
    }
  }

  private String streamingUri(ProductSubscription subscription) {
    String path = wsUri(exchangeSpecification);

    if (subscription != null) path += "stream?streams=" + buildSubscriptionStreams(subscription);

    return path;
  }

  protected String wsUri(ExchangeSpecification exchangeSpecification) {
    if (exchangeSpecification.getOverrideWebsocketApiUri() != null) {
      return exchangeSpecification.getOverrideWebsocketApiUri();
    }

    boolean useSandbox = usingSandbox();

    if (useSandbox) {
      return WS_USD_TESTNET_FUTURES_API_BASE_URI;
    } else {
      return WS_USD_FUTURES_API_BASE_URI;
    }
  }


  private String buildSubscriptionStreams(ProductSubscription subscription) {
    return Stream.of(
            buildSubscriptionStrings(
                subscription.getTicker(), tickerRealtimeSubscriptionParameter ? BinanceSubscriptionType.BOOK_TICKER.getType() : BinanceSubscriptionType.TICKER.getType()),
            buildSubscriptionStrings(
                subscription.getOrderBook(), BinanceSubscriptionType.DEPTH.getType()),
            buildSubscriptionStrings(
                subscription.getTrades(), BinanceSubscriptionType.TRADE.getType()))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining("/"));
  }

  private String buildSubscriptionStrings(
      List<Instrument> currencyPairs, String subscriptionType) {
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

  private static Stream<String> subscriptionStrings(List<Instrument> currencyPairs) {
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

    if (subscriptions.hasAuthenticated()) {
      if (exchangeSpecification.getApiKey() == null) {
        throw new IllegalArgumentException("API key required for authenticated streams");
      }

      LOG.info("Connecting to authenticated web socket");
      BinanceFuturesStreaming binance =
          ExchangeRestProxyBuilder.forInterface(
                  BinanceFuturesStreaming.class, getExchangeSpecification())
              .build();
      userDataChannel =
          new BinanceUserDataChannel(binance, exchangeSpecification.getApiKey(), onApiCall);
      try {
        completables.add(createAndConnectUserDataService(userDataChannel.getListenKey()));
      } catch (BinanceUserDataChannel.NoActiveChannelException e) {
        throw new IllegalStateException("Failed to establish user data channel", e);
      }
    }

    streamingMarketDataService = new BinanceFuturesStreamingMarketDataService(
        streamingService,
        getBinanceOrderBookProvider(),
        onApiCall,
        orderBookUpdateFrequencyParameter,
        tickerRealtimeSubscriptionParameter,
        oderBookFetchLimitParameter);
    streamingTradeService = new BinanceFuturesStreamingTradeService(userDataStreamingService);

    return Completable.concat(completables)
        .doOnComplete(() -> streamingMarketDataService.openSubscriptions(subscriptions, new KlineSubscription(emptyMap())))
        .doOnComplete(() -> streamingTradeService.openSubscriptions());
  }

  private Completable createAndConnectUserDataService(String listenKey) {
    boolean useSandbox = Boolean.TRUE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX));
    userDataStreamingService = BinanceFuturesUserDataStreamingService.create(listenKey, useSandbox);
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
                                        streamingTradeService.setUserDataStreamingService(
                                            userDataStreamingService);
                                      });
                            });
                  });
            });
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

  @Override
  public StreamingTradeService getStreamingTradeService() {
    return streamingTradeService;
  }

  private BinanceStreamingService createStreamingService(ProductSubscription subscription) {
    BinanceFuturesStreamingService streamingService = new BinanceFuturesStreamingService(streamingUri(subscription), subscription);
    applyStreamingSpecification(getExchangeSpecification(), streamingService);
    return streamingService;
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
  public Observable<ConnectionStateModel.State> connectionStateObservable() {
    return streamingService.subscribeConnectionState();
  }

  @Override
  public boolean isAlive() {
    return (streamingService != null && streamingService.isSocketOpen() || userDataStreamingService != null && userDataStreamingService.isSocketOpen());
  }

  @Override
  public void useCompressedMessages(boolean compressedMessages) {
    streamingService.useCompressedMessages(compressedMessages);
  }
}
