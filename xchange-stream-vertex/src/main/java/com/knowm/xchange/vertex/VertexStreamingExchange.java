package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.MoreObjects;
import com.knowm.xchange.vertex.api.VertexArchiveApi;
import com.knowm.xchange.vertex.dto.RewardsList;
import com.knowm.xchange.vertex.dto.RewardsRequest;
import com.knowm.xchange.vertex.dto.Symbol;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;
import static com.knowm.xchange.vertex.VertexStreamingService.UNLIMITED;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.*;

public class VertexStreamingExchange extends BaseExchange implements StreamingExchange {

  public static final String USE_LEVERAGE = "useLeverage";
  public static final String MAX_SLIPPAGE_RATIO = "maxSlippageRatio";
  public static final String BLEND_LIQUIDATION_TRADES = "blendLiquidationTrades";
  public static final String DEFAULT_SUB_ACCOUNT = "default";
  public static final String PLACE_ORDER_VALID_UNTIL_MS_PROP = "placeOrderValidUntilMs";
  public static final String GATEWAY_WEBSOCKET = "gatewayWebsocketUrl";
  public static final String QUERY_WEBSOCKET = "queryWebsocketUrl";
  public static final String SUBSCRIPTIONS_WEBSOCKET = "subscriptionWebsocketUrl";
  public static final String CUSTOM_SYMBOLS = "customSymbols";
  public static final String CUSTOM_HOST = "customHost";
  private static final ObjectMapper json = new ObjectMapper();

  private VertexStreamingService subscriptionStream;
  private VertexStreamingMarketDataService streamingMarketDataService;
  private VertexStreamingTradeService streamingTradeService;

  private boolean useTestnet;

  private long chainId;

  private String endpointContract;

  private List<String> bookContracts;

  private VertexStreamingService orderStream;
  private VertexStreamingService queryStream;
  private VertexProductInfo productInfo;

  private final Map<Long, TopOfBookPrice> marketPrices = new ConcurrentHashMap<>();
  private final Map<Long, InstrumentDefinition> increments = new HashMap<>();

  private final Set<Long> spotProducts = new TreeSet<>();
  private final Set<Long> perpProducts = new TreeSet<>();

  private Observable<JsonNode> allQueryMessages;
  private Observable<JsonNode> allOrderMessages;
  private VertexExchange restExchange;


  @Override
  public ExchangeSpecification getDefaultExchangeSpecification() {
    ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass());
    exchangeSpecification.setHost(VertexExchange.getGatewayHost(useTestnet));
    exchangeSpecification.setExchangeName("Vertex");
    exchangeSpecification.setExchangeDescription("Vertex - One DEX. Everything you need.");
    return exchangeSpecification;
  }


  public void applySpecification(ExchangeSpecification exchangeSpecification) {
    this.useTestnet = Boolean.TRUE.equals(Boolean.parseBoolean(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX))));

    if (useTestnet) {
      exchangeSpecification.setHost(VertexExchange.getGatewayHost(useTestnet));
    }

    this.restExchange = new VertexExchange();
    restExchange.applySpecification(exchangeSpecification);

    super.applySpecification(exchangeSpecification);
  }


  @Override
  public void remoteInit() throws ExchangeException {

    if (!queryStream.isSocketOpen() && !queryStream.connect().blockingAwait(10, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout waiting for connection");
    }

    Symbol[] symbols = restExchange.queryAPI().symbols().data.symbols.values().toArray(new Symbol[0]);

    if (exchangeSpecification.getExchangeSpecificParametersItem(CUSTOM_SYMBOLS) != null) {
      String customSymbolsJson = exchangeSpecification.getExchangeSpecificParametersItem(CUSTOM_SYMBOLS).toString();

      try {
        Symbol[] customSymbols = json.readerForArrayOf(Symbol.class).readValue(customSymbolsJson);
        logger.info("Custom symbols: {}", Arrays.toString(customSymbols));
        Set<String> customSymbolStrings = Arrays.stream(customSymbols).map(Symbol::getSymbol).collect(Collectors.toSet());
        // replace symbols in the array with customSymbols
        symbols = Arrays.stream(symbols).filter(s -> !customSymbolStrings.contains(s.getSymbol())).toArray(Symbol[]::new);
        symbols = ArrayUtils.addAll(symbols, customSymbols);
      } catch (JsonProcessingException e) {
        logger.error("Failed to parse custom symbols: " + customSymbolsJson, e);
      }
    }

    ArrayList<Query> queries = new ArrayList<>();
    ArrayList<Query> priceQueries = new ArrayList<>();
    logger.info("Loading contract data and current prices");
    queries.add(new Query("{\"type\":\"contracts\"}",
        data1 -> {
          chainId = Long.parseLong(data1.get("chain_id").asText());
          endpointContract = data1.get("endpoint_addr").asText();
          bookContracts = new ArrayList<>();
          data1.withArray("book_addrs").elements().forEachRemaining(node -> bookContracts.add(node.asText()));
        }, (code, error) -> {
      throw new ExchangeException("Error loading contract data: " + code + " " + error);
    }));

    List<BigDecimal> takerFees = new ArrayList<>();
    List<BigDecimal> makerFees = new ArrayList<>();
    AtomicReference<BigDecimal> takerSequencerFee = new AtomicReference<>();
    String walletAddress = exchangeSpecification.getApiKey();
    if (StringUtils.isNotEmpty(walletAddress)) {
      queries.add(new Query("{\"type\":\"fee_rates\", \"sender\": \"" + buildSender(walletAddress, getSubAccountOrDefault()) + "\"}",
          feeData -> {
            readX18DecimalArray(feeData, "taker_fee_rates_x18", takerFees);
            readX18DecimalArray(feeData, "maker_fee_rates_x18", makerFees);
            takerSequencerFee.set(readX18Decimal(feeData, "taker_sequencer_fee"));
          }, (code, error) -> {
        throw new ExchangeException("Error loading fee data: " + code + " " + error);
      }));
    } else {
      throw new ExchangeException("API key must be provided via exchange specification");
    }
    Arrays.sort(symbols, Comparator.comparing(Symbol::getProduct_id));
    Symbol[] finalSymbols = symbols;
    logger.info("Available symbols: {}", Arrays.toString(symbols));

    queries.add(new Query("{\"type\":\"all_products\"}", productData -> {
      processProductIncrements(productData.withArray("spot_products"), spotProducts);
      processProductIncrements(productData.withArray("perp_products"), perpProducts);

      //TODO - pull this from API when available
      BigDecimal interestFee = BigDecimal.valueOf(0.2);
      productInfo = new VertexProductInfo(spotProducts, finalSymbols, takerFees, makerFees, takerSequencerFee.get(), interestFee);

      Query marketPricesQuery = new Query("{\"type\":\"market_prices\", \"product_ids\": " + productInfo.getProductsIds().stream().filter(id -> id != 0).collect(Collectors.toList()) + "}",
          priceData -> priceData.get("market_prices").forEach(price -> {
            long productId = price.get("product_id").asLong();
            marketPrices.put(productId, new TopOfBookPrice(readX18Decimal(price, "bid_x18"), readX18Decimal(price, "ask_x18")));
          }), (code, error) -> logger.error("Error loading market prices: " + code + " " + error));
      priceQueries.add(marketPricesQuery);


    }, (code, error) -> {
      throw new ExchangeException("Error loading product data: " + code + " " + error);
    }));

    submitQueries(queries.toArray(new Query[0]));

    submitQueries(priceQueries.toArray(new Query[0]));

  }

  private void processProductIncrements(ArrayNode spotProducts, Set<Long> productSet) {
    for (JsonNode spotProduct : spotProducts) {
      long productId = spotProduct.get("product_id").asLong();
      if (productId == 0) { // skip USDC product
        continue;
      }
      productSet.add(productId);
      JsonNode bookInfo = spotProduct.get("book_info");
      BigDecimal quantityIncrement = x18ToDecimal(new BigInteger(bookInfo.get("size_increment").asText()));
      BigDecimal priceIncrement = x18ToDecimal(new BigInteger(bookInfo.get("price_increment_x18").asText()));
      increments.put(productId, new InstrumentDefinition(priceIncrement, quantityIncrement));
    }
  }

  public RewardsList queryRewards(String walletAddress) {
    return restExchange.archiveApi().rewards(new RewardsRequest(new RewardsRequest.RewardAddress(walletAddress)));
  }

  public synchronized void submitQueries(Query... queries) {

    Observable<JsonNode> stream = subscribeToAllQueryMessages();

    for (Query query : queries) {
      CountDownLatch responseLatch = new CountDownLatch(1);
      Disposable subscription = stream.subscribe(resp -> {
        JsonNode requestType = resp.get("request_type");
        if (requestType != null && (requestType.textValue().startsWith("query_") || requestType.textValue().startsWith("q_"))) {
          try {
            JsonNode data = resp.get("data");
            JsonNode error = resp.get("error");
            JsonNode errorCode = resp.get("error_code");
            JsonNode status = resp.get("status");
            boolean success = status != null && status.asText().equals("success");

            if (!success) {
              query.getErrorHandler().accept(errorCode.asInt(-1), error.asText("Unknown error"));
            } else {
              query.getRespHandler().accept(data);
              logger.info("Query response " + data.toPrettyString());
            }
          } catch (Throwable t) {
            logger.error("Query error running " + query.getQueryMsg(), t);
            query.getErrorHandler().accept(-1, "Query error running " + query.getQueryMsg() + ": " + t.getMessage());
          } finally {
            responseLatch.countDown();
          }

        }
      }, (err) -> {
        logger.error("Query error running " + query.getQueryMsg(), err);
        query.getErrorHandler().accept(-1, "Query error running " + query.getQueryMsg() + ": " + err.getMessage());
      });

      try {
        logger.info("Sending query " + query.getQueryMsg());
        queryStream.sendMessage(query.getQueryMsg());
        if (!responseLatch.await(20, TimeUnit.SECONDS)) {
          query.getErrorHandler().accept(-1, "Timed out after 20 seconds waiting for response for " + query.getQueryMsg());
        }
      } catch (InterruptedException e) {
        logger.error("Failed to get contract data due to timeout");
      } finally {
        subscription.dispose();
      }
    }
  }

  public Observable<JsonNode> subscribeToAllQueryMessages() {
    if (allQueryMessages == null) {
      allQueryMessages = queryStream.subscribeChannel(ALL_MESSAGES);
    }
    return allQueryMessages;
  }


  public Observable<JsonNode> subscribeToAllOrderMessages() {
    if (allOrderMessages == null) {
      allOrderMessages = orderStream.subscribeChannel(ALL_MESSAGES);
    }
    return allOrderMessages;
  }

  @Override
  protected void initServices() {
    this.subscriptionStream = getSubscriptionStream();
    this.orderStream = getOrderStream();
    this.queryStream = getQueryStream();


  }

  private VertexStreamingService getOrderStream() {
    VertexStreamingService streamingService = new VertexStreamingService(getOrderWsUrl(), exchangeSpecification, this, UNLIMITED, "[orders]");
    applyStreamingSpecification(getExchangeSpecification(), streamingService);
    return streamingService;
  }

  private VertexStreamingService getQueryStream() {
    VertexStreamingService streamingService = new VertexStreamingService(getQueryWsUrl(), exchangeSpecification, this, UNLIMITED, "[queries]");
    applyStreamingSpecification(getExchangeSpecification(), streamingService);
    return streamingService;
  }

  private VertexStreamingService getSubscriptionStream() {
    VertexStreamingService streamingService = new VertexStreamingService(getSubscriptionWsUrl(), exchangeSpecification, this, VertexStreamingService.TEN_PER_SECOND, "[subscriptions]");
    applyStreamingSpecification(getExchangeSpecification(), streamingService);
    return streamingService;
  }

  private String getOrderWsUrl() {
    return VertexExchange.overrideOrDefault(GATEWAY_WEBSOCKET, "wss://" + VertexExchange.getGatewayHost(useTestnet) + "/v1/ws", exchangeSpecification);
  }

  private String getQueryWsUrl() {
    return VertexExchange.overrideOrDefault(QUERY_WEBSOCKET, "wss://" + VertexExchange.getGatewayHost(useTestnet) + "/v1/ws", exchangeSpecification);
  }

  private String getSubscriptionWsUrl() {
    return VertexExchange.overrideOrDefault(SUBSCRIPTIONS_WEBSOCKET, "wss://" + VertexExchange.getGatewayHost(useTestnet) + "/v1/subscribe", exchangeSpecification);
  }


  @Override
  public StreamingMarketDataService getStreamingMarketDataService() {
    if (this.streamingMarketDataService == null) {
      this.streamingMarketDataService = new VertexStreamingMarketDataService(subscriptionStream, productInfo, this);
    }
    return streamingMarketDataService;
  }

  @Override
  public VertexStreamingTradeService getStreamingTradeService() {
    if (this.streamingTradeService == null) {
      this.streamingTradeService = new VertexStreamingTradeService(orderStream, subscriptionStream, getExchangeSpecification(), productInfo, chainId, bookContracts, this, endpointContract, getStreamingMarketDataService());
    }
    return streamingTradeService;
  }

  @Override
  public TradeService getTradeService() {
    return getStreamingTradeService();
  }

  @Override
  public MarketDataService getMarketDataService() {
    return new VertexMarketDataService(restExchange, productInfo);
  }

  @Override
  public Observable<ConnectionStateModel.State> connectionStateObservable() {
    return subscriptionStream.subscribeConnectionState();
  }

  @Override
  public Observable<Throwable> reconnectFailure() {
    return subscriptionStream.subscribeReconnectFailure();
  }

  @Override
  public Observable<Object> connectionSuccess() {
    return subscriptionStream.subscribeConnectionSuccess();
  }

  @Override
  public Completable connect(ProductSubscription... args) {
    List<VertexStreamingService> services = new ArrayList<>();
    if (!orderStream.isSocketOpen()) {
      services.add(orderStream);
    }
    if (!subscriptionStream.isSocketOpen()) {
      services.add(subscriptionStream);
    }
    if (!queryStream.isSocketOpen()) {
      services.add(queryStream);
    }
    return Completable.mergeArray(services.stream().map(VertexStreamingService::connect).toArray(Completable[]::new));
  }

  @Override
  public Completable disconnect() {
    return Completable.concatArray(subscriptionStream.disconnect(), orderStream.disconnect(), queryStream.disconnect());
  }

  @Override
  public boolean isAlive() {
    return subscriptionStream.isSocketOpen() && orderStream.isSocketOpen() && queryStream.isSocketOpen();
  }

  @Override
  public void useCompressedMessages(boolean compressedMessages) {
    subscriptionStream.useCompressedMessages(compressedMessages);
  }

  public TopOfBookPrice getMarketPrice(Long productId) {
    return marketPrices.get(productId);
  }

  public void setMarketPrice(Long productId, TopOfBookPrice price) {
    marketPrices.put(productId, price);
  }


  /**
   * size and price increments for a product
   */
  public InstrumentDefinition getIncrements(Long productId) {
    return increments.get(productId);
  }

  public VertexArchiveApi getRestClient() {
    return restExchange.archiveApi();
  }

  /*
   Fee charged on taker trades in BPS (always positive)
   */
  public BigDecimal getTakerTradeFee(long productId) {
    return productInfo.takerTradeFee(productId);
  }

  /*
  Rebate paid on maker trades in BPS (always positive)
  */
  public BigDecimal getMakerTradeFee(long productId) {
    return productInfo.makerTradeFee(productId);
  }

  String getSubAccountOrDefault() {
    return MoreObjects.firstNonNull(exchangeSpecification.getUserName(), DEFAULT_SUB_ACCOUNT);
  }

  /*
   Fixed USDC fee charged on every taker trade
   */
  public BigDecimal getTakerFee() {
    return productInfo.takerSequencerFee();
  }

  public long getChainId() {
    return chainId;
  }

  public String getEndpointContract() {
    return endpointContract;
  }

  public VertexProductInfo getProductInfo() {
    return productInfo;
  }
}
