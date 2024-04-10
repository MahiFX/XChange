package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

  private static final int RETRIES = 20;
  private static final int RETRY_DELAY_MILLI = 500;

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
  private final Cache<String, Order> orderCache = CacheBuilder.newBuilder().maximumSize(1000).build();
  private final Map<Instrument, AtomicReference<BigInteger>> indexCounters = new ConcurrentHashMap<>();
  private final Map<Pair<Instrument, Boolean>, BigInteger> balanceCache = new ConcurrentHashMap<>();
  private final Disposable allMessageSubscription;
  private final StreamingMarketDataService marketDataService;
  private final Scheduler liquidationScheduler = Schedulers.io();

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
    this.slippage = exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO) != null ? Double.parseDouble(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO))) : DEFAULT_MAX_SLIPPAGE_RATIO;
    this.useLeverage = exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE) != null ? Boolean.parseBoolean(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE))) : DEFAULT_USE_LEVERAGE;
    this.placeOrderValidUntilMs = exchangeSpecification.getExchangeSpecificParametersItem(PLACE_ORDER_VALID_UNTIL_MS_PROP) != null ? Integer.parseInt(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(PLACE_ORDER_VALID_UNTIL_MS_PROP))) : 60000;

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

      if (typeNode != null && (typeNode.textValue().startsWith("q_") || typeNode.textValue().startsWith("query_"))) {
        return; // ignore query responses that are handled in VertexStreamingExchange
      }

      JsonNode statusNode = resp.get("status");
      JsonNode signatureNode = resp.get("signature");

      if (statusNode == null || typeNode == null || signatureNode == null) {
        logger.error("Unable to handle incomplete response: {}", resp);
        return;
      }

      boolean success = "success".equals(statusNode.asText());
      String type = typeNode.asText(); // FIXME map of req -> response types
      if (type.startsWith("e_")) {
        type = type.replaceFirst("e_", "execute_");
      }
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

    try {
      getOpenPositions().getOpenPositions().forEach(pos -> {
        BigInteger balance = pos.getType() == OpenPosition.Type.LONG ? decimalToX18(pos.getSize()) : decimalToX18(pos.getSize()).negate();
        balanceCache.put(Pair.of(pos.getInstrument(), false), balance);
        balanceCache.put(Pair.of(pos.getInstrument(), true), BigInteger.ZERO);
      });
    } catch (IOException e) {
      logger.error("Error getting initial open positions", e);
    }
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
    return subscribeToOrderUpdate(instrument).map(resp -> {

      String reason = resp.get("reason").asText();

      String orderId = resp.get("digest").asText();
      Order knownOrder = orderCache.getIfPresent(orderId);
      Order.Builder builder = new LimitOrder.Builder(null, instrument);
      BigDecimal remaining = readX18Decimal(resp, "amount");

      Instant timestamp = NanoSecondsDeserializer.parse(resp.get("timestamp").asText());
      Order.OrderStatus status = getOrderStatus(remaining, reason);
      return builder.id(orderId)
          .instrument(instrument)
          .orderStatus(status)
          .originalAmount(knownOrder == null ? null : knownOrder.getOriginalAmount())
          .remainingAmount(remaining)
          .timestamp(new Date(timestamp.toEpochMilli()))
          .build();
    });
  }

  private static Order.OrderStatus getTradeStatus(BigDecimal remaining, BigDecimal filled, BigDecimal original) {
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

  private static Order.OrderStatus getOrderStatus(BigDecimal remaining, String reason) {
    if ("cancelled".equals(reason)) return Order.OrderStatus.CANCELED;

    Order.OrderStatus status = Order.OrderStatus.UNKNOWN;
    if ("filled".equals(reason)) {
      status = isZero(remaining) ? Order.OrderStatus.FILLED : Order.OrderStatus.PARTIALLY_FILLED;
    } else if ("placed".equals(reason)) {
      status = Order.OrderStatus.NEW;
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


  private Observable<JsonNode> subscribeToOrderUpdate(Instrument instrument) {
    subscriptionStream.authenticate();
    long productId = productInfo.lookupProductId(instrument);

    String subAccount = exchange.getSubAccountOrDefault();

    String channel = "order_update." + productId + "." + buildSender(exchangeSpecification.getApiKey(), subAccount);
    return fillSubscriptions.computeIfAbsent(channel, c -> subscriptionStream.subscribeChannel(channel));
  }


  private Observable<JsonNode> subscribeToPositionChange(Instrument instrument) {
    long productId = productInfo.lookupProductId(instrument);

    String subAccount = exchange.getSubAccountOrDefault();

    String channel = "position_change." + productId + "." + buildSender(exchangeSpecification.getApiKey(), subAccount);
    return positionSubscriptions.computeIfAbsent(channel, c -> subscriptionStream.subscribeChannel(channel).filter(resp -> {
      boolean isLp = resp.get("is_lp").asBoolean();
      BigInteger newBal = new BigInteger(resp.get("amount").asText());
      Pair<Instrument, Boolean> balanceKey = Pair.of(instrument, isLp);
      BigInteger prevBal = balanceCache.computeIfAbsent(balanceKey, p -> newBal);
      boolean balanceMatch = newBal.equals(prevBal);
      balanceCache.put(balanceKey, newBal);
      if (!balanceMatch) {
        logger.info("Balance change for {}{}: {} -> {}", instrument, isLp ? " LP" : "", x18ToDecimal(prevBal), x18ToDecimal(newBal));
      } else {
        logger.debug("No balance change for {}{}: {}", instrument, isLp ? " LP" : "", x18ToDecimal(newBal));
      }
      return !balanceMatch;
    }));
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

    if (Boolean.parseBoolean(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(BLEND_LIQUIDATION_TRADES)))) {
      return mergeInLiquidationTrades(instrument, productId, tradeStream);
    }
    return tradeStream;
  }

  private Observable<UserTrade> mergeInLiquidationTrades(Instrument instrument, long productId, Observable<UserTrade> tradeStream) {
    AtomicReference<BigInteger> indexCounter = indexCounters.computeIfAbsent(instrument, (i) -> new AtomicReference<BigInteger>(BigInteger.ZERO));

    String subAccount = getSubAccountOrDefault();

    Instant startTime = Instant.now();
    boolean isSpot = productInfo.isSpot(instrument);

    String ourSender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    JsonNode eventQuery = events("liquidate_subaccount", ourSender, productId, 50);

    Observable<UserTrade> liquidations = subscribeToPositionChange(instrument)
        .debounce(RETRY_DELAY_MILLI, TimeUnit.MILLISECONDS, liquidationScheduler).switchMap(c -> {
          BigDecimal newBalance = readX18Decimal(c, "amount");
          logger.info("Checking for {} liquidation events since index {}. New {} position: {}", instrument, indexCounter.get(), instrument, newBalance);
              // For each change, create an inner observable that retries until new balance is seen or max attempts is reached
              return Observable.just(c).flatMap(change -> {
                    synchronized (indexCounter) {
                      BigInteger maxIdx = indexCounter.get();
                      boolean seenExpectedBalance = false;
                      List<UserTrade> liquidationTrades = new ArrayList<>();
                      JsonNode events_resp = exchange.getRestClient().indexerRequest(eventQuery);
                      Map<BigInteger, JsonNode> txnLookup = newTransactionsIdLookup(events_resp, maxIdx);
                      if (!txnLookup.isEmpty()) {
                        ArrayNode events = events_resp.withArray("events");
                        logger.debug("Found {} new liquidation events, in {} transactions", events.size(), txnLookup.size());
                        Iterator<JsonNode> iterator = events.iterator();
                        // reverse the events iterator as they are returned most recent first
                        List<JsonNode> eventsList = new ArrayList<>();
                        while (iterator.hasNext()) {
                          eventsList.add(iterator.next());
                        }
                        Collections.reverse(eventsList);

                        Set<String> newTxns = new CopyOnWriteArraySet<>();
                        for (JsonNode event : eventsList) {
                          BigInteger idx = new BigInteger(event.get("submission_idx").asText());
                          if (idx.compareTo(maxIdx) > 0) {
                            maxIdx = idx;

                            JsonNode transaction = txnLookup.get(idx);
                            if (transaction == null) continue;
                            JsonNode timestampNode = transaction.get("timestamp");
                            Instant timestamp = EpochSecondsDeserializer.parse(timestampNode.textValue());
                            boolean before = timestamp.isBefore(startTime);
                            if (before) {
                              continue;
                            }

                            String instrumentFieldName = isSpot ? "spot" : "perp";

                            JsonNode postBalanceNode = event.get("post_balance").get(instrumentFieldName).get("balance");
                            BigDecimal postBalance = readX18Decimal(postBalanceNode, "amount");
                            if (postBalance.equals(newBalance)) {
                              seenExpectedBalance = true;
                            }
                            JsonNode preBalanceNode = event.get("pre_balance").get(instrumentFieldName).get("balance");
                            BigDecimal preBalance = readX18Decimal(preBalanceNode, "amount");

                            BigDecimal tradeQuantity = postBalance.subtract(preBalance);
                            // We receive events for liquidations of other pairs, even with the product_id filter
                            if (tradeQuantity.compareTo(BigDecimal.ZERO) == 0) {
                              continue;
                            }

                            String txnId = ORDER_ID_HASHER.hashString("liquidation:" + productId + ":" + idx, StandardCharsets.UTF_8).toString();
                            if (newTxns.contains(txnId)) {
                              continue;
                            }
                            newTxns.add(txnId);

                            UserTrade build = buildLiquidationTrade(instrument, event, instrumentFieldName, transaction, ourSender, tradeQuantity, txnId, subAccount, timestamp);
                            liquidationTrades.add(build);
                          }
                        }
                        indexCounter.set(maxIdx);
                      }

                      Observable<UserTrade> tradeObservable = Observable.fromArray(liquidationTrades.toArray(new UserTrade[0]));
                      if (!seenExpectedBalance) {
                        logger.debug("New balance for {} not seen in liquidation events, retrying", instrument);
                        return Observable.concat(tradeObservable, Observable.error(new RuntimeException("Expected balance not seen")));
                      } else {
                        logger.info("Expected balance for {} seen in liquidations events, publishing {} trades", instrument, liquidationTrades.size());
                        return tradeObservable;
                      }


                    }
                  }).retryWhen(errors -> errors
                      .zipWith(Observable.range(1, RETRIES), (n, i) -> i)
                      .flatMap(retry -> Observable.timer(RETRY_DELAY_MILLI, TimeUnit.MILLISECONDS, liquidationScheduler)))
                  .doOnComplete(() -> logger.info("Retry limit reached for {} liquidation events, no liquidations found", instrument));
            }
        );

    return Observable.merge(tradeStream, liquidations);
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
      if (subAccountInfo != null) {
        subAccountInfo.withArray("spot_balances").elements().forEachRemaining(bal -> addBalance(positions, bal, summary));
        subAccountInfo.withArray("perp_balances").elements().forEachRemaining(bal -> addBalance(positions, bal, summary));
      }
      return new OpenPositions(positions);
    } catch (InterruptedException ignored) {
      return new OpenPositions(Collections.emptyList());
    }


  }

  private JsonNode events(String eventType, String sender, long productId, int limit) {
    String jsonString = String.format("{\"events\": {" +
        "\"subaccount\": \"%s\"," +
        "\"product_ids\": [%s]," +
        "\"limit\": {\"txs\": %d}, " +
        "\"event_types\": [\"%s\"]" +
        "}}", sender, productId, limit, eventType);

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
        if (balance.equals(BigDecimal.ZERO)) {
          continue;
        }
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
    if (!subscriptionStream.isSocketOpen()) {
      throw new ExchangeException("Can't place order, event stream is disconnected");
    }
    Instrument instrument = marketOrder.getInstrument();
    long productId = productInfo.lookupProductId(instrument);

    BigInteger expiration = getExpiration(marketOrder.getOrderFlags());

    InstrumentDefinition increments = exchange.getIncrements(productId);
    BigDecimal priceIncrement = increments.getPriceIncrement();
    price = roundToIncrement(price, priceIncrement);
    BigInteger priceAsInt = decimalToX18(price);

    BigDecimal quantity = getQuantity(marketOrder);
//    BigDecimal quantityIncrement = increments.getQuantityIncrement();
//    if (quantity.abs().compareTo(quantityIncrement) < 0) {
//      throw new IllegalArgumentException("Quantity must be greater than increment");
//    }
//    quantity = roundToIncrement(quantity, quantityIncrement);
    BigInteger quantityAsInt = decimalToX18(quantity);

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
      orderCache.put(signatureAndDigest.getDigest(), marketOrder);
    } catch (Throwable e) {
      logger.error("Failed to place order : {}", orderMessage, e);
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
    if (!subscriptionStream.isSocketOpen()) {
      throw new ExchangeException("Can't place order, event stream is disconnected");
    }
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
      logger.error("Failed to cancel order ({}): {}", id, cancelReq, e);
      return Collections.emptyList();

    }
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
      String subAccount = exchange.getSubAccountOrDefault();
      List<Long> productsIds;
      List<LimitOrder> orders = new ArrayList<>();
      if (params instanceof OpenOrdersParamInstrument) {
        CurrencyPair instrument = (CurrencyPair) ((OpenOrdersParamInstrument) params).getInstrument();
        long productId = productInfo.lookupProductId(instrument);
        productsIds = List.of(productId);
      } else {
        productsIds = productInfo.getProductsIds().stream().filter(id -> id != 0).collect(Collectors.toList());
      }

      exchange.submitQueries(new Query(openOrders(productsIds, subAccount), (data) -> {
        data.withArray("product_orders").forEach(productOrder -> {
          long productId = productOrder.get("product_id").asLong();
          String priceX18 = "price_x18";
          productOrder.withArray("orders").forEach(order -> {

            BigDecimal price = readX18Decimal(order, priceX18);
            BigDecimal amount = readX18Decimal(order, "amount");
            BigDecimal unfilledAmount = readX18Decimal(order, "unfilled_amount");

            Date placedAt = new Date(Instant.ofEpochSecond(order.get("placed_at").asLong()).toEpochMilli());
            BigDecimal filled = amount.subtract(unfilledAmount);

            Instrument instrument = productInfo.lookupInstrument(productId);

            LimitOrder.Builder builder = new LimitOrder.Builder(amount.compareTo(BigDecimal.ZERO) > 0 ? Order.OrderType.BID : Order.OrderType.ASK, instrument)
                .id(order.get("digest").asText())
                .limitPrice(price)
                .originalAmount(amount)
                .remainingAmount(unfilledAmount)
                .orderStatus(getTradeStatus(unfilledAmount, filled, amount))
                .cumulativeAmount(filled)
                .timestamp(placedAt);
            orders.add(builder.build());
          });
        });
        responseLatch.complete(new OpenOrders(orders));
      }, (code, error) -> {
        if ("Too Many Requests".equalsIgnoreCase(error)) {
          logger.warn("Failed to get open orders: {}", error);
          responseLatch.complete(new OpenOrders(Collections.emptyList()));
        } else {
          responseLatch.completeExceptionally(new ExchangeException("Failed to get open orders: " + error));
        }
      }));


      return responseLatch.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | CancellationException interrupted) {
      logger.warn("Interrupted waiting for open orders response: {}", interrupted.getMessage());
      return new OpenOrders(Collections.emptyList());
    } catch (TimeoutException e) {
      throw new IOException("Timeout waiting for open orders response");
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

  }

  private String openOrders(List<Long> productIds, String subAccount) {
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
    return String.format("{\"type\": \"orders\",\"sender\": \"%s\",\"product_ids\": %s}", sender, productIds);
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


  private static Map<BigInteger, JsonNode> newTransactionsIdLookup(JsonNode events_resp, BigInteger maxIdx) {
    ArrayNode txns = events_resp.withArray("txs");
    Map<BigInteger, JsonNode> txnMap = new HashMap<>();
    txns.forEach(tx -> {
      BigInteger submission_idx = new BigInteger(tx.get("submission_idx").asText());
      if (submission_idx.compareTo(maxIdx) > 0) {
        txnMap.put(submission_idx, tx);
      }
    });
    return txnMap;
  }

  private static UserTrade buildLiquidationTrade(Instrument instrument, JsonNode event, String instrumentFieldName, JsonNode transaction, String ourSender, BigDecimal tradeQuantity, String id, String subAccount, Instant timestamp) {
    JsonNode product = event.get("product").get(instrumentFieldName);
    double oraclePrice = readX18Decimal(product, "oracle_price_x18").doubleValue();

    Order.OrderType side;
    double price;
    double maintenanceWeight;

    String liquidator = transaction.get("tx").get("liquidate_subaccount").get("sender").textValue();
    boolean wasLiquidator = ourSender.equals(liquidator);

    // If we liquidated a long position then we will have bought it from the liquidatee and our position will increase
    // If we had a long position liquidated then we will have sold and our position will decrease
    boolean liquidatedPositionWasLong = wasLiquidator && tradeQuantity.compareTo(BigDecimal.ZERO) > 0 || !wasLiquidator && tradeQuantity.compareTo(BigDecimal.ZERO) < 0;

    if (liquidatedPositionWasLong) {
      maintenanceWeight = readX18Decimal(product.get("risk"), "long_weight_maintenance_x18").doubleValue();
      side = wasLiquidator ? Order.OrderType.BID : Order.OrderType.ASK;
    } else {
      maintenanceWeight = readX18Decimal(product.get("risk"), "short_weight_maintenance_x18").doubleValue();
      side = wasLiquidator ? Order.OrderType.ASK : Order.OrderType.BID;
    }

    //https://vertex-protocol.gitbook.io/docs/basics/liquidations-and-insurance-fund
    // e.g. 25% = 0.25 TODO read from API
    double insuranceFundRate = 0.25;
    double insuranceMultiplier = 1 - insuranceFundRate;

    price = oraclePrice - (oraclePrice * ((1 - maintenanceWeight) / 5) * insuranceMultiplier);

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

    return builder.build();
  }
}
