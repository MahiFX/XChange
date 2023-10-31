package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.knowm.xchange.vertex.dto.*;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.SignatureAndDigest;
import com.knowm.xchange.vertex.signing.schemas.CancelOrdersSchema;
import com.knowm.xchange.vertex.signing.schemas.CancelProductOrdersSchema;
import com.knowm.xchange.vertex.signing.schemas.PlaceOrderSchema;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.account.OpenPositions;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.*;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParamInstrument;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.knowm.xchange.vertex.VertexStreamingExchange.*;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.*;

public class VertexStreamingTradeService implements StreamingTradeService, TradeService {

  public static final double DEFAULT_MAX_SLIPPAGE_RATIO = 0.005;
  public static final ObjectMapper MAPPER = new ObjectMapper();
  private static final boolean DEFAULT_USE_LEVERAGE = false;
  public static final Consumer<Ticker> NO_OP = ticker -> {
  };
  public static final HashFunction ORDER_ID_HASHER = Hashing.murmur3_32_fixed();
  private final Logger logger = LoggerFactory.getLogger(VertexStreamingTradeService.class);

  private final VertexStreamingService requestResponseStream;
  private final VertexStreamingService subscriptionStream;
  private final ExchangeSpecification exchangeSpecification;
  private final ObjectMapper mapper;

  private final VertexProductInfo productInfo;
  private final long chainId;
  private final List<String> bookContracts;
  private final VertexStreamingExchange exchange;
  private final String endpointContract;
  private final double slippage;
  private final boolean useLeverage;
  private final int placeOrderValidUntilMs;
  private final Map<Pair<String, String>, CompletableFuture<JsonNode>> responses = new ConcurrentHashMap<>();
  private final Map<Long, Disposable> tickerSubscriptions = new ConcurrentHashMap<>();
  private final Map<String, Observable<JsonNode>> fillSubscriptions = new ConcurrentHashMap<>();
  private final Map<String, Observable<JsonNode>> positionSubscriptions = new ConcurrentHashMap<>();
  private final Disposable allMessageSubscription;
  private final StreamingMarketDataService marketDataService;
  private final Scheduler liquidationScheduler = Schedulers.single();

  public VertexStreamingTradeService(VertexStreamingService requestResponseStream, VertexStreamingService subscriptionStream, ExchangeSpecification exchangeSpecification, VertexProductInfo productInfo, long chainId, List<String> bookContracts, VertexStreamingExchange exchange, String endpointContract, StreamingMarketDataService marketDataService) {
    this.requestResponseStream = requestResponseStream;
    this.subscriptionStream = subscriptionStream;
    this.exchangeSpecification = exchangeSpecification;
    this.productInfo = productInfo;
    this.chainId = chainId;
    this.bookContracts = bookContracts;
    this.endpointContract = endpointContract;
    this.exchange = exchange;
    this.marketDataService = marketDataService;
    this.mapper = StreamingObjectMapperHelper.getObjectMapper();
    this.slippage = exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO) != null ? Double.parseDouble(exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO).toString()) : DEFAULT_MAX_SLIPPAGE_RATIO;
    this.useLeverage = exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE) != null ? Boolean.parseBoolean(exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE).toString()) : DEFAULT_USE_LEVERAGE;
    this.placeOrderValidUntilMs = exchangeSpecification.getExchangeSpecificParametersItem(PLACE_ORDER_VALID_UNTIL_MS_PROP) != null ? (int) exchangeSpecification.getExchangeSpecificParametersItem(PLACE_ORDER_VALID_UNTIL_MS_PROP) : 60000;

    exchange.connectionStateObservable().subscribe(
        s -> {
          if (!ConnectionStateModel.State.CLOSED.equals(s)) {
            return;
          }

          Collection<CompletableFuture<JsonNode>> futures = responses.values();

          if (futures.isEmpty()) {
            return;
          }

          logger.info("Cancelling {} pending operations due to {} state", futures.size(), s);

          futures.forEach(f -> f.cancel(false));
          responses.clear();
        },
        t -> logger.error("Connection state observer error", t)
    );

    this.allMessageSubscription = exchange.subscribeToAllMessages().subscribe(resp -> {
      JsonNode typeNode = resp.get("request_type");

      if (typeNode != null && typeNode.textValue().startsWith("query")) {
        return; // ignore query responses that are handled in VertexStreamingExchange
      }

      JsonNode statusNode = resp.get("status");
      JsonNode signatureNode = resp.get("signature");

      if (statusNode == null || typeNode == null || signatureNode == null) {
        logger.error("Unable to handle incomplete response: {}", resp);
        return;
      }

      boolean success = "success".equals(statusNode.asText());
      String type = typeNode.asText();
      String signature = signatureNode.asText();

      CompletableFuture<JsonNode> responseFuture = responses.remove(Pair.of(type, signature));

      if (responseFuture != null) {
        if (success) {
          logger.info("Received success for {} ({}): {}", type, signature, resp);
          responseFuture.complete(resp);
        } else {
          logger.error("Received error for {} ({}): {}", type, signature, resp);
          responseFuture.completeExceptionally(new ExchangeException(resp.get("error").asText()));
        }

      } else {
        if (success) {
          logger.warn("Received success for unknown {} ({}): {}", type, signature, resp);
        } else {
          logger.error("Received error for unknown {} ({}): {}", type, signature, resp);
        }

      }
    });
  }

  @SuppressWarnings("unused")
  public void disconnect() {
    allMessageSubscription.dispose();
    tickerSubscriptions.values().stream().filter(Disposable::isDisposed).forEach(Disposable::dispose);
    if (requestResponseStream.isSocketOpen()) {
      if (!requestResponseStream.disconnect().blockingAwait(10, TimeUnit.SECONDS)) {
        logger.warn("Timeout waiting for disconnect");
      }
    }
  }

  @Override
  public String placeLimitOrder(LimitOrder limitOrder) {
    BigDecimal price = limitOrder.getLimitPrice();

    return placeOrder(limitOrder, price);
  }

  @Override
  public String placeMarketOrder(MarketOrder marketOrder) {
    long productId = productInfo.lookupProductId(marketOrder.getInstrument());

    BigDecimal price = getPrice(marketOrder, productId);

    return placeOrder(marketOrder, price);
  }

  @Override
  public Observable<Order> getOrderChanges(Instrument instrument, Object... args) {
    return subscribeToFills(instrument).map(resp -> {
      boolean isBid = resp.get("is_bid").asBoolean();
      Order.Builder builder = new LimitOrder.Builder(isBid ? Order.OrderType.BID : Order.OrderType.ASK, instrument);
      String orderId = resp.get("order_digest").asText();
      BigDecimal original = readX18Decimal(resp, "original_qty");
      BigDecimal remaining = readX18Decimal(resp, "remaining_qty");
      BigDecimal price = readX18Decimal(resp, "price");
      BigDecimal filled = readX18Decimal(resp, "filled_qty");
      Instant timestamp = NanoSecondsDeserializer.parse(resp.get("timestamp").asText());
      String respSubAccount = resp.get("subaccount").asText();
      Order.OrderStatus status = getOrderStatus(remaining, filled, original);
      return builder.id(orderId)
          .instrument(instrument)
          .originalAmount(original)
          .cumulativeAmount(filled)
          .orderStatus(status)
          .averagePrice(price)
          .remainingAmount(remaining)
          .userReference(respSubAccount)
          .timestamp(new Date(timestamp.toEpochMilli()))
          .build();
    });
  }

  private static Order.OrderStatus getOrderStatus(BigDecimal remaining, BigDecimal filled, BigDecimal original) {
    Order.OrderStatus status;
    if (isZero(remaining) || filled.equals(original)) {
      status = Order.OrderStatus.FILLED;
    } else if (isZero(filled) || remaining.equals(original)) {
      status = Order.OrderStatus.NEW;
    } else {
      status = Order.OrderStatus.PARTIALLY_FILLED;
    }
    return status;
  }

  private static boolean isZero(BigDecimal remaining) {
    return remaining.compareTo(BigDecimal.ZERO) == 0;
  }

  private Observable<JsonNode> subscribeToFills(Instrument instrument) {
    long productId = productInfo.lookupProductId(instrument);

    String subAccount = exchange.getSubAccountOrDefault();

    String channel = "fill." + productId + "." + buildSender(exchangeSpecification.getApiKey(), subAccount);
    return fillSubscriptions.computeIfAbsent(channel, c -> subscriptionStream.subscribeChannel(channel));
  }


  private Observable<JsonNode> subscribeToPositionChange(Instrument instrument) {
    long productId = productInfo.lookupProductId(instrument);

    String subAccount = exchange.getSubAccountOrDefault();

    String channel = "position_change." + productId + "." + buildSender(exchangeSpecification.getApiKey(), subAccount);
    return positionSubscriptions.computeIfAbsent(channel, c -> subscriptionStream.subscribeChannel(channel));
  }

  @Override
  public Observable<UserTrade> getUserTrades(Instrument instrument, Object... args) {
    long productId = productInfo.lookupProductId(instrument);

    Observable<UserTrade> tradeStream = subscribeToFills(instrument).map(resp -> {
          boolean isBid = resp.get("is_bid").asBoolean();
          boolean isTaker = resp.get("is_taker").asBoolean();
          UserTrade.Builder builder = new UserTrade.Builder();

          String orderId = resp.get("order_digest").asText();
          BigDecimal price = readX18Decimal(resp, "price");
          BigDecimal filled = readX18Decimal(resp, "filled_qty");

          if (isZero(filled)) {
            return Optional.<UserTrade>empty();
          }
          String timestampText = resp.get("timestamp").asText();
          Instant timestamp = NanoSecondsDeserializer.parse(timestampText);
          String respSubAccount = resp.get("subaccount").asText();
          BigDecimal orderQty = readX18Decimal(resp, "original_qty");
          BigDecimal remaining = readX18Decimal(resp, "remaining_qty");
          BigDecimal totalFilled = orderQty.subtract(remaining);
          String filledPercentage = totalFilled.divide(orderQty, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.HALF_DOWN).toPlainString();
          boolean isFirstFill = totalFilled.compareTo(filled) == 0;

          BigDecimal fee = calcFee(isTaker, filled, productId, price, isFirstFill);
          return Optional.of(builder.id(ORDER_ID_HASHER.hashString(orderId + ":" + totalFilled.toPlainString() + ":" + price.toPlainString(), Charsets.UTF_8) + "-" + filledPercentage)
              .instrument(instrument)
              .originalAmount(filled)
              .orderId(orderId)
              .price(price)
              .type(isBid ? Order.OrderType.BID : Order.OrderType.ASK)
              .orderUserReference(respSubAccount)
              .timestamp(new Date(timestamp.toEpochMilli()))
              .feeCurrency(Currency.USDC)
              .feeAmount(fee)
              .creationTimestamp(new Date())
              .build());
        })
        .filter(Optional::isPresent)
        .map(Optional::get);

    if (Boolean.parseBoolean(exchangeSpecification.getExchangeSpecificParametersItem(BLEND_LIQUIDATION_TRADES).toString())) {
      AtomicLong indexCounter = new AtomicLong();

      String subAccount = getSubAccountOrDefault();

      Instant startTime = Instant.now();
      boolean isSpot = productInfo.isSpot(instrument);

      String ourSender = buildSender(exchangeSpecification.getApiKey(), subAccount);

      Observable<UserTrade> liquidations = subscribeToPositionChange(instrument)
          .debounce(2, TimeUnit.SECONDS, liquidationScheduler).flatMap((change) -> {
            synchronized (indexCounter) {
              logger.info("Checking for " + instrument + " liquidation events since " + indexCounter.get());
              List<UserTrade> liquidationTrades = new ArrayList<>();
              Set<String> newIds = new HashSet<>();
              JsonNode events_resp = exchange.getRestClient().indexerRequest(events("liquidate_subaccount", instrument));
              ArrayNode txns = events_resp.withArray("txs");
              Map<Long, JsonNode> txnMap = new HashMap<>();
              txns.forEach(tx -> txnMap.put(Long.valueOf(tx.get("submission_idx").textValue()), tx));

              ArrayNode events = events_resp.withArray("events");
              Iterator<JsonNode> iterator = events.iterator();
              // reverse the events iterator as they are returned most recent first
              List<JsonNode> eventsList = new ArrayList<>();
              while (iterator.hasNext()) {
                eventsList.add(iterator.next());
              }
              Collections.reverse(eventsList);

              long maxIdx = indexCounter.get();
              for (JsonNode event : eventsList) {
                long idx = event.get("submission_idx").asLong();
                if (idx > maxIdx) {
                  maxIdx = idx;

                  JsonNode transaction = txnMap.get(idx);
                  if (transaction == null) continue;
                  JsonNode timestampNode = transaction.get("timestamp");
                  Instant timestamp = EpochSecondsDeserializer.parse(timestampNode.textValue());
                  if (timestamp.isBefore(startTime)) {
                    continue;
                  }

                  String instrumentFieldName = isSpot ? "spot" : "perp";

                  JsonNode postBalanceNode = event.get("post_balance").get(instrumentFieldName).get("balance");
                  BigDecimal postBalance = readX18Decimal(postBalanceNode, "amount");

                  JsonNode preBalanceNode = event.get("pre_balance").get(instrumentFieldName).get("balance");
                  BigDecimal preBalance = readX18Decimal(preBalanceNode, "amount");

                  BigDecimal tradeQuantity = postBalance.subtract(preBalance);
                  // We receive events for liquidations of other pairs, even with the product_id filter
                  if (tradeQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                  }
                  JsonNode product = event.get("product").get(instrumentFieldName);
                  double oraclePrice = readX18Decimal(product, "oracle_price_x18").doubleValue();

                  Order.OrderType side;
                  double price;
                  BigDecimal weight;

                  String liquidator = transaction.get("tx").get("liquidate_subaccount").get("sender").textValue();
                  boolean wasLiquidator = ourSender.equals(liquidator);

                  // If we liquidated a long position then we will have bought it from the liquidatee and our position will increase
                  // If we had a long position liquidated then we will have sold and our position will decrease
                  boolean liquidatedPositionWasLong = wasLiquidator && tradeQuantity.compareTo(BigDecimal.ZERO) > 0 || !wasLiquidator && tradeQuantity.compareTo(BigDecimal.ZERO) < 0;

                  if (liquidatedPositionWasLong) {
                    weight = readX18Decimal(product.get("risk"), "long_weight_maintenance_x18");
                    side = wasLiquidator ? Order.OrderType.BID : Order.OrderType.ASK;
                  } else {
                    weight = readX18Decimal(product.get("risk"), "short_weight_maintenance_x18");
                    side = wasLiquidator ? Order.OrderType.ASK : Order.OrderType.BID;
                  }

                  //https://vertex-protocol.gitbook.io/docs/basics/liquidations-and-insurance-fund
                  // e.g. 25% = 0.25 TODO read from API
                  double insuranceFundRate = 0.25;
                  double insuranceMultiplier = 1 - insuranceFundRate;

                  price = oraclePrice - (oraclePrice * (1 - weight.doubleValue()) * 0.5 * insuranceMultiplier);

                  String id = ORDER_ID_HASHER.hashString("liquidation:" + productId + ":" + idx, StandardCharsets.UTF_8).toString();
                  if (newIds.contains(id)) {
                    continue;
                  }
                  newIds.add(id);
                  UserTrade.Builder builder = new UserTrade.Builder();

                  builder.id(id)
                      .instrument(instrument)
                      .originalAmount(tradeQuantity.abs())
                      .orderId(id + "-liquidation")
                      .price(BigDecimal.valueOf(price))
                      .type(side)
                      .orderUserReference(subAccount)
                      .timestamp(new Date(timestamp.toEpochMilli()))
                      .creationTimestamp(new Date());

                  liquidationTrades.add(builder.build());
                }
              }
              indexCounter.set(maxIdx);
              return Observable.fromArray(liquidationTrades.toArray(new UserTrade[0]));
            }
          });
      return Observable.merge(tradeStream, liquidations);
    }
    return tradeStream;
  }

  private BigDecimal calcFee(boolean isTaker, BigDecimal filled, long productId, BigDecimal price, boolean isFirstFill) {
    BigDecimal bpsFee = isTaker ? exchange.getTakerTradeFee(productId) : exchange.getMakerTradeFee(productId);
    BigDecimal lhsFee = filled.multiply(bpsFee);

    //Fixed sequencer fee is only charged on first fill per order
    BigDecimal usdcFee = lhsFee.multiply(price).setScale(2, RoundingMode.HALF_UP);
    if (isTaker && isFirstFill) {
      usdcFee = usdcFee.add(exchange.getTakerFee());
    }
    return isTaker ? usdcFee : usdcFee.negate();
  }

  public OpenPositions getOpenPositions() throws IOException {
    CountDownLatch response = new CountDownLatch(1);

    String subAccount = exchange.getSubAccountOrDefault();
    AtomicReference<JsonNode> subAccountInfoHolder = new AtomicReference<>();
    exchange.submitQueries(new Query(subAccountInfo(subAccount), newValue -> {
      subAccountInfoHolder.set(newValue);
      response.countDown();
    }, (code, error) -> {
      logger.error("Error getting subaccount info: {} {}", code, error);
      response.countDown();
    }));

    try {
      if (!response.await(10, TimeUnit.SECONDS)) {
        throw new IOException("Timeout waiting for open positions response");
      }


      JsonNode summary = exchange.getRestClient().indexerRequest(summary(subAccount));

      JsonNode subAccountInfo = subAccountInfoHolder.get();
      List<OpenPosition> positions = new ArrayList<>();
      subAccountInfo.withArray("spot_balances").elements().forEachRemaining(bal -> addBalance(positions, bal, summary));
      subAccountInfo.withArray("perp_balances").elements().forEachRemaining(bal -> addBalance(positions, bal, summary));

      return new OpenPositions(positions);
    } catch (InterruptedException ignored) {
      return new OpenPositions(Collections.emptyList());
    }


  }

  private JsonNode events(String eventType, Instrument instrument) {
    String subAccount = exchange.getSubAccountOrDefault();
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    long productId = productInfo.lookupProductId(instrument);
    String jsonString = String.format("{\"events\": {" +
        "\"subaccount\": \"%s\"," +
        "\"product_ids\": [%s]," +
        "\"event_types\": [\"%s\"]" +
        "}}", sender, productId, eventType);

    try {
      return MAPPER.readTree(jsonString);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private JsonNode summary(String subAccount) {
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    String jsonString = String.format("{\"summary\": {\"subaccount\": \"%s\"}}", sender);

    try {
      return MAPPER.readTree(jsonString);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private void addBalance(List<OpenPosition> positions, JsonNode bal, JsonNode summary) {
    int productId = bal.get("product_id").asInt();
    Instrument instrument = productInfo.lookupInstrument(productId);
    if (instrument == null) {
      logger.warn("No instrument found for product id {}", productId);
      return;
    }
    BigDecimal position = readX18Decimal(bal.get("balance"), "amount");
    if (isZero(position)) {
      return;
    }
    BigDecimal price = findPrice(productId, summary);
    positions.add(new OpenPosition(instrument, position.compareTo(BigDecimal.ZERO) >= 0 ? OpenPosition.Type.LONG : OpenPosition.Type.SHORT, position.abs(), price, null, null));
  }

  private BigDecimal findPrice(int productId, JsonNode summary) {
    Iterator<JsonNode> events = summary.withArray("events").elements();

    while (events.hasNext()) {
      JsonNode event = events.next();
      if (event.get("product_id").asInt() == productId) {
        JsonNode postBalance = event.get("post_balance");
        BigDecimal balance = readX18Decimal(MoreObjects.firstNonNull(postBalance.get("perp"), postBalance.get("spot")).get("balance"), "amount");
        BigDecimal netUnrealised = readX18Decimal(event, "net_entry_unrealized");
        return netUnrealised.divide(balance, RoundingMode.HALF_UP).abs();
      }
    }
    return null;
  }

  private String subAccountInfo(String subAccount) {
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    return String.format("{\"type\": \"subaccount_info\",\"subaccount\": \"%s\"}", sender);
  }

  private String placeOrder(Order marketOrder, BigDecimal price) {
    Instrument instrument = marketOrder.getInstrument();
    long productId = productInfo.lookupProductId(instrument);

    BigInteger expiration = getExpiration(marketOrder.getOrderFlags());

    InstrumentDefinition increments = exchange.getIncrements(productId);
    BigDecimal priceIncrement = increments.getPriceIncrement();
    price = roundToIncrement(price, priceIncrement);
    BigInteger priceAsInt = convertToInteger(price);

    BigDecimal quantity = getQuantity(marketOrder);
    BigDecimal quantityIncrement = increments.getQuantityIncrement();
    if (quantity.abs().compareTo(quantityIncrement) < 0) {
      throw new IllegalArgumentException("Quantity must be greater than increment");
    }
    quantity = roundToIncrement(quantity, quantityIncrement);
    BigInteger quantityAsInt = convertToInteger(quantity);

    String subAccount = exchange.getSubAccountOrDefault();
    String nonce = buildNonce(placeOrderValidUntilMs);
    String walletAddress = exchangeSpecification.getApiKey();
    String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

    String bookContract = bookContracts.get((int) productId);
    PlaceOrderSchema orderSchema = PlaceOrderSchema.build(chainId,
        bookContract,
        Long.valueOf(nonce),
        sender,
        expiration,
        quantityAsInt,
        priceAsInt);
    SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

    VertexPlaceOrderMessage orderMessage = new VertexPlaceOrderMessage(new VertexPlaceOrder(
        productId,
        new VertexOrder(sender, priceAsInt.toString(), quantityAsInt.toString(), expiration.toString(), nonce),
        signatureAndDigest.getSignature(),
        productInfo.isSpot(instrument) ? useLeverage : null));

    logger.info("Send order {} -> {} (valid for {}ms)", marketOrder, signatureAndDigest, placeOrderValidUntilMs);

    try {
      sendWebsocketMessage(orderMessage);

    } catch (Throwable e) {
      logger.error("Failed to place order : " + orderMessage, e);
      throw new ExchangeException(e);

    }

    return signatureAndDigest.getDigest();
  }

  private JsonNode sendWebsocketMessage(VertexRequest messageObj) throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
    String requestType = messageObj.getRequestType();
    String signature = messageObj.getSignature();

    String message = mapper.writeValueAsString(messageObj);

    logger.info("Sending {} ({}): {}", requestType, signature, message);

    CompletableFuture<JsonNode> responseFuture = getResponseFuture(requestType, signature);

    requestResponseStream.sendMessage(message);

    try {
      return responseFuture.get(5000, TimeUnit.MILLISECONDS);

    } catch (Throwable e) {
      responses.remove(Pair.of(requestType, signature));
      throw e;

    }
  }

  private CompletableFuture<JsonNode> getResponseFuture(String requestType, String signature) {
    CompletableFuture<JsonNode> responseFuture = new CompletableFuture<>();
    CompletableFuture<JsonNode> oldFuture = responses.putIfAbsent(Pair.of(requestType, signature), responseFuture);
    Preconditions.checkState(oldFuture == null, "Already pending a response for %s (%s): %s", requestType, signature, oldFuture);
    return responseFuture;
  }

  private BigInteger getExpiration(Set<Order.IOrderFlags> orderFlags) {
    BigInteger timeInForce = BigInteger.ZERO; // resting
    Instant expiryTime = Instant.MAX; // No expiry
    if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_IOC)) {
      timeInForce = BigInteger.ONE;
      expiryTime = Instant.now().plus(5, ChronoUnit.SECONDS); // Force IOC/FOK timeouts
    } else if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_FOK)) {
      timeInForce = BigInteger.valueOf(2);
      expiryTime = Instant.now().plus(5, ChronoUnit.SECONDS); // Force IOC/FOK timeouts
    } else if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_POS_ONLY)) {
      timeInForce = BigInteger.valueOf(3);
    }

    BigInteger expiry = BigInteger.valueOf(expiryTime.getEpochSecond());
    BigInteger tifMask = timeInForce.shiftLeft(62);
    return expiry.or(tifMask);
  }

  private BigDecimal getPrice(Order order, long productId) {
    BigDecimal price;
    if (order instanceof LimitOrder) {
      price = ((LimitOrder) order).getLimitPrice();
    } else {
      // Make sure we have a subscription to the ticker for market prices
      tickerSubscriptions.computeIfAbsent(productId, id -> marketDataService.getTicker(order.getInstrument()).forEach(NO_OP));
      TopOfBookPrice bidOffer = exchange.getMarketPrice(productId);
      boolean isSell = order.getType().equals(Order.OrderType.ASK);
      if (isSell) {
        BigDecimal bid = bidOffer.getBid();
        // subtract max slippage from bid
        price = bid.subtract(bid.multiply(BigDecimal.valueOf(slippage)));
      } else {
        BigDecimal offer = bidOffer.getOffer();
        // add max slippage to offer
        price = offer.add(offer.multiply(BigDecimal.valueOf(slippage)));
      }
    }
    return price;
  }

  @Override
  public Collection<String> cancelAllOrders(CancelAllOrders orderParams) {
    return doCancel(orderParams);
  }

  @Override
  public boolean cancelOrder(CancelOrderParams params) {
    return !doCancel(params).isEmpty();
  }

  private List<String> doCancel(CancelOrderParams params) {
    String id = getOrderId(params);
    Instrument instrument = getInstrument(params);

    VertexRequest cancelReq;

    if (StringUtils.isNotEmpty(id) && instrument != null) {

      long productId = productInfo.lookupProductId(instrument);

      String subAccount = exchange.getSubAccountOrDefault();
      String nonce = buildNonce(60000);
      String walletAddress = exchangeSpecification.getApiKey();
      String sender = VertexModelUtils.buildSender(walletAddress, subAccount);
      long[] productIds = {productId};
      String[] digests = {id};

      CancelOrdersSchema orderSchema = CancelOrdersSchema.build(chainId, endpointContract, Long.valueOf(nonce), sender, productIds, digests);
      SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

      cancelReq = new VertexCancelOrdersMessage(new CancelOrders(
          new Tx(sender, productIds, digests, nonce),
          signatureAndDigest.getSignature()
      ));


    } else if (params instanceof CancelAllOrders || instrument != null) {
      List<Long> productIds = new ArrayList<>();
      if (instrument != null) {
        productIds.add(productInfo.lookupProductId(instrument));
      }

      String subAccount = exchange.getSubAccountOrDefault();
      String nonce = buildNonce(60000);
      String walletAddress = exchangeSpecification.getApiKey();
      String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

      long[] productIdsArray = productIds.stream().mapToLong(l -> l).toArray();

      CancelProductOrdersSchema cancelAllSchema = CancelProductOrdersSchema.build(chainId, endpointContract, Long.valueOf(nonce), sender, productIdsArray);

      SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(cancelAllSchema);

      cancelReq = new VertexCancelProductOrdersMessage(new CancelProductOrders(
          new Tx(sender, productIdsArray, null, nonce),
          signatureAndDigest.getSignature()
      ));


    } else {
      throw new IllegalArgumentException(
          "CancelOrderParams must implement some of CancelOrderByIdParams, CancelOrderByInstrument, CancelOrderByCurrencyPair, CancelAllOrders interfaces.");
    }


    try {
      JsonNode resp = sendWebsocketMessage(cancelReq);
      ArrayNode array = resp.get("data").withArray("cancelled_orders");
      List<String> digests = new ArrayList<>();
      array.forEach(order -> digests.add(order.get("digest").asText()));
      return digests;

    } catch (Throwable e) {
      logger.error("Failed to cancel order (" + id + "): " + cancelReq, e);
      boolean alreadyCancelled = isAlreadyCancelled(Throwables.getRootCause(e));
      return alreadyCancelled ? List.of(id) : Collections.emptyList();

    }
  }

  private boolean isAlreadyCancelled(Throwable throwable) {
    // Treat this as a successful cancel as automatic/unsolicited cancellations are not notified
    String message = throwable.getMessage();
    return message != null && message.matches(".*Order with the provided digest .* could not be found.*");
  }

  private String getOrderId(CancelOrderParams params) {
    if (params instanceof CancelOrderByIdParams) {
      return ((CancelOrderByIdParams) params).getOrderId();
    }
    return null;
  }

  private Instrument getInstrument(CancelOrderParams params) {
    if (params instanceof CancelOrderByCurrencyPair || params instanceof CancelOrderByInstrument) {
      return params instanceof CancelOrderByCurrencyPair ? ((CancelOrderByCurrencyPair) params).getCurrencyPair() : ((CancelOrderByInstrument) params).getInstrument();
    }
    return null;
  }

  private String getSubAccountOrDefault() {
    return MoreObjects.firstNonNull(exchangeSpecification.getUserName(), DEFAULT_SUB_ACCOUNT);
  }

  @Override
  public OpenOrders getOpenOrders() throws IOException {
    return getOpenOrders(null);
  }

  @Override
  public OpenOrders getOpenOrders(OpenOrdersParams params) throws IOException {
    try {


      CompletableFuture<OpenOrders> responseLatch = new CompletableFuture<>();

      if (params instanceof OpenOrdersParamInstrument) {
        CurrencyPair instrument = (CurrencyPair) ((OpenOrdersParamInstrument) params).getInstrument();
        long productId = productInfo.lookupProductId(instrument);

        String subAccount = exchange.getSubAccountOrDefault();
        exchange.submitQueries(new Query(openOrders(productId, subAccount), (data) -> {
          List<LimitOrder> orders = new ArrayList<>();
          data.withArray("orders").elements().forEachRemaining(order -> {
            String priceX18 = "price_x18";
            BigDecimal price = readX18Decimal(order, priceX18);
            BigDecimal amount = readX18Decimal(order, "amount");
            BigDecimal unfilledAmount = readX18Decimal(order, "unfilled_amount");

            Date placedAt = new Date(Instant.ofEpochSecond(order.get("placed_at").asLong()).toEpochMilli());
            BigDecimal filled = amount.subtract(unfilledAmount);
            LimitOrder.Builder builder = new LimitOrder.Builder(amount.compareTo(BigDecimal.ZERO) > 0 ? Order.OrderType.BID : Order.OrderType.ASK, instrument)
                .id(order.get("digest").asText())
                .limitPrice(price)
                .originalAmount(amount)
                .remainingAmount(unfilledAmount)
                .orderStatus(getOrderStatus(unfilledAmount, filled, amount))
                .cumulativeAmount(filled)
                .timestamp(placedAt);
            orders.add(builder.build());

          });
          responseLatch.complete(new OpenOrders(orders));
        }, (code, error) -> responseLatch.completeExceptionally(new ExchangeException("Failed to get open orders: " + error))));

        return responseLatch.get(10, TimeUnit.SECONDS);

      } else {

        String subAccount = getSubAccountOrDefault();
        List<LimitOrder> orders = new ArrayList<>();

        List<Query> queries = new ArrayList<>();
        List<Long> productsIds = productInfo.getProductsIds().stream().filter(id -> id != 0).collect(Collectors.toList());
        CountDownLatch pendingQueries = new CountDownLatch(productsIds.size());
        for (Long productId : productsIds) {
          Instrument instrument = productInfo.lookupInstrument(productId);
          queries.add(new Query(openOrders(productId, subAccount), (data) -> {
            data.withArray("orders").elements().forEachRemaining(order -> {
              String priceX18 = "price_x18";
              BigDecimal price = readX18Decimal(order, priceX18);
              BigDecimal amount = readX18Decimal(order, "amount");
              BigDecimal unfilledAmount = readX18Decimal(order, "unfilled_amount");

              Date placedAt = new Date(Instant.ofEpochSecond(order.get("placed_at").asLong()).toEpochMilli());
              BigDecimal filled = amount.subtract(unfilledAmount);
              LimitOrder.Builder builder = new LimitOrder.Builder(amount.compareTo(BigDecimal.ZERO) > 0 ? Order.OrderType.BID : Order.OrderType.ASK, instrument)
                  .id(order.get("digest").asText())
                  .limitPrice(price)
                  .originalAmount(amount)
                  .remainingAmount(unfilledAmount)
                  .orderStatus(getOrderStatus(unfilledAmount, filled, amount))
                  .cumulativeAmount(filled)
                  .timestamp(placedAt);
              orders.add(builder.build());

            });
            pendingQueries.countDown();
          }, (code, error) -> {
            pendingQueries.countDown();
            responseLatch.completeExceptionally(new ExchangeException("Failed to get open orders: " + error));
          }));
        }

        exchange.submitQueries(queries.toArray(new Query[0]));
        if (!pendingQueries.await(10, TimeUnit.SECONDS)) {
          throw new IOException("Timeout waiting for open orders response");
        }
        responseLatch.complete(new OpenOrders(orders));
      }

      return responseLatch.get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | CancellationException ignored) {
      return new OpenOrders(Collections.emptyList());
    } catch (TimeoutException e) {
      throw new IOException("Timeout waiting for open orders response");
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

  }

  private String openOrders(long productId, String subAccount) {
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    return String.format("{\"type\": \"subaccount_orders\",\"sender\": \"%s\",\"product_id\": %d}", sender, productId);
  }

  private BigDecimal getQuantity(Order order) {
    BigDecimal quantityAsInt = order.getOriginalAmount();
    if (order.getType().equals(Order.OrderType.ASK)) {
      quantityAsInt = quantityAsInt.multiply(BigDecimal.valueOf(-1));
    }
    return quantityAsInt;
  }

  public static BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
    if (isZero(increment)) return value;
    BigDecimal divided = value.divide(increment, 0, RoundingMode.FLOOR);
    return divided.multiply(increment);
  }


  @Override
  public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
    return getOrderChanges((Instrument) currencyPair, args);
  }

  @Override
  public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
    return getUserTrades((Instrument) currencyPair, args);
  }


}
