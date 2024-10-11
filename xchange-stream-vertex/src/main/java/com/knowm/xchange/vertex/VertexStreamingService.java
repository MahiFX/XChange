package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.SignatureAndDigest;
import com.knowm.xchange.vertex.signing.schemas.StreamAuthentication;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.ExchangeSpecification;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static com.knowm.xchange.vertex.VertexExchange.overrideOrDefault;
import static com.knowm.xchange.vertex.VertexStreamingExchange.CUSTOM_HOST;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.buildSender;

public class VertexStreamingService extends JsonNettyStreamingService {

  //Channel to use to subscribe to all response
  public static final String ALL_MESSAGES = "all_messages";
  private static final int MAX_FRAME_KB = 1024 * 256;
  public static final RateLimiter TEN_PER_SECOND = RateLimiter.of("vertex-10-per-sec", RateLimiterConfig
      .custom().limitForPeriod(10).limitRefreshPeriod(Duration.ofSeconds(1)).build());

  public static final RateLimiter UNLIMITED = RateLimiter.of("vertex-unlimited", RateLimiterConfig
      .custom().limitForPeriod(Integer.MAX_VALUE).limitRefreshPeriod(Duration.ofSeconds(1)).build());

  private final AtomicLong reqCounter = new AtomicLong(1);
  private final String apiUrl;
  private final ExchangeSpecification exchangeSpecification;
  private final VertexStreamingExchange exchange;
  private boolean authenticated;
  private boolean wasAuthenticated;
  private Observable<JsonNode> allMessages;

  public VertexStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification, VertexStreamingExchange exchange, RateLimiter rateLimit, String name) {
    super(apiUrl, MAX_FRAME_KB, Duration.ofSeconds(5), Duration.ofSeconds(1), 15, rateLimit, name);
    this.apiUrl = apiUrl;
    this.exchangeSpecification = exchangeSpecification;
    this.exchange = exchange;
  }

  @Override
  protected DefaultHttpHeaders getCustomHeaders() {
    String customHost = overrideOrDefault(CUSTOM_HOST, null, exchangeSpecification);
    if (StringUtils.isNotEmpty(customHost)) {
      DefaultHttpHeaders headers = super.getCustomHeaders();
      headers.add("Host", customHost);
      return headers;
    }
    return super.getCustomHeaders();
  }

  @Override
  public String getSubscriptionUniqueId(String channelName, Object... args) {

    if (channelName.startsWith("order_update")) {
      // Drop subaccount from channel as response message only have order digest on them
      String[] components = channelName.split("\\.");
      return components[0] + "." + components[1];
    }
    return super.getSubscriptionUniqueId(channelName, args);
  }

  @Override
  protected String getChannelNameFromMessage(JsonNode message) {
    JsonNode type = message.get("type");
    JsonNode productId = message.get("product_id");
    JsonNode subaccount = message.get("subaccount");
    if (type != null) {
      if (productId != null) {
        if (subaccount != null) {
          return type.asText() + "." + productId.asText() + "." + subaccount.asText();
        }
        return type.asText() + "." + productId.asText();
      }
      return type.asText();
    } else {
      return ALL_MESSAGES;
    }

  }

  @Override
  public String getSubscribeMessage(String channelName, Object... args) {
    if (channelName.startsWith(ALL_MESSAGES)) {
      return null;
    }
    String[] typeAndProduct = channelName.split("\\.");
    long reqId = reqCounter.incrementAndGet();
//    AtomicReference<Disposable> responseSub = new AtomicReference<>();
//
//    responseSub.set(allMessages.subscribe((message) -> {
//      logger.debug("Subscription response: {}", message);
//      if (message.get("id").asLong() == reqId) {
//        if (message.get("error") != null) {
//          logger.error("Error subscribing to channel " + channelName + ": " + message.get("error"));
//        } else {
//          logger.info("Subscribed to channel " + channelName + " successfully");
//        }
//        responseSub.get().dispose();
//      }
//    }));

    String subAccount = exchange.getSubAccountOrDefault();
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);

    return "{\n" +
        "  \"method\": \"subscribe\",\n" +
        "  \"stream\": {\n" +
        "    \"type\": \"" + typeAndProduct[0] + "\"\n" +
        productIdField(typeAndProduct) +
        subAccountField(sender) +
        "  },\n" +
        "  \"id\": " + reqId + "\n" +
        "}";
  }

  private static String productIdField(String[] typeAndProduct) {
    return typeAndProduct.length > 1 ? ", \"product_id\": " + typeAndProduct[1] + "\n" : "";
  }

  private String subAccountField(String sender) {
    return ",\"subaccount\": \"" + sender + "\"\n";

  }

  @Override
  public String getUnsubscribeMessage(String channelName, Object... args) {
    if (channelName.startsWith(ALL_MESSAGES)) {
      return null;
    }
    String[] typeAndProduct = channelName.split("\\.");
    long reqId = reqCounter.incrementAndGet();

    String subAccount = exchange.getSubAccountOrDefault();
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);

    return "{\n" +
        "  \"method\": \"unsubscribe\",\n" +
        "  \"stream\": {\n" +
        "    \"type\": \"" + typeAndProduct[0] + "\"\n" +
        productIdField(typeAndProduct) +
        subAccountField(sender) +
        "  },\n" +
        "  \"id\": " + reqId + "\n" +
        "}";
  }

  public synchronized void authenticate() {
    wasAuthenticated = true;
    if (authenticated) return;
    String subAccount = exchange.getSubAccountOrDefault();

    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);


    long chainId = exchange.getChainId();
    String endpointContract = exchange.getEndpointContract();

    if (chainId == 0 || endpointContract == null) {
      throw new IllegalStateException("ChainId or EndpointContract not available. Cannot authenticate");
    }

    Instant expiry = Instant.now().plus(20, ChronoUnit.SECONDS);
    String timestamp = String.valueOf(expiry.toEpochMilli());
    StreamAuthentication streamAuth = StreamAuthentication.build(chainId,
        endpointContract,
        sender,
        BigInteger.valueOf(expiry.toEpochMilli()));
    SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(streamAuth);

    LOG.info("Authenticating stream");

    CompletableFuture<JsonNode> responseLatch = new CompletableFuture<>();
    long requestId = reqCounter.incrementAndGet();
    Disposable responseSub = allMessages.subscribe(value -> {
      LOG.debug("Authentication response: {}", value);
      if (value.get("id").asLong() == requestId) {
        responseLatch.complete(value);
      } else if (value.get("error") != null) {
        responseLatch.complete(value);
      }
    });

    try {
      sendMessage("{\n" +
          "  \"method\": \"authenticate\",\n" +
          "  \"id\": " + requestId + ",\n" +
          "  \"tx\": {\n" +
          "    \"sender\": \"" + sender + "\",\n" +
          "    \"expiration\": \"" + timestamp + "\"\n" +
          "  },\n" +
          "  \"signature\": \"" + signatureAndDigest.getSignature() + "\"\n" +
          "}");

      JsonNode response = responseLatch.get(10, TimeUnit.SECONDS);
      JsonNode error = response.get("error");
      if (error != null) {
        if (!error.textValue().contains("already authenticated")) {
          throw new RuntimeException("Authentication error: " + error);
        }
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for authentication response");
      return;

    } catch (TimeoutException | ExecutionException e) {
      throw new RuntimeException("Authentication timeout", e);
    } finally {
      responseSub.dispose();
    }
    authenticated = true;
  }

  @Override
  public void resubscribeChannels() {
    authenticated = false;
    allMessages = subscribeChannel(ALL_MESSAGES).share();

    if (wasAuthenticated) {
      authenticate();
    }
    super.resubscribeChannels();
  }

  @Override
  public Completable disconnect() {
    if (isSocketOpen()) {
      LOG.info("Disconnecting {}", apiUrl);
      return super.disconnect();
    } else {
      LOG.info("Already disconnected {}", apiUrl);
      return Completable.complete();
    }
  }
}
