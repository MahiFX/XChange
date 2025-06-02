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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.knowm.xchange.vertex.dto.VertexModelUtils.buildSender;

public class VertexStreamingService extends JsonNettyStreamingService {

  //Channel to use to subscribe to all response
  public static final String ALL_MESSAGES = "all_messages";
  private static final int MAX_FRAME_KB = 1024 * 256;
  public static final RateLimiter TEN_PER_SECOND = RateLimiter.of("vertex-10-per-sec", RateLimiterConfig
      .custom().timeoutDuration(Duration.ofSeconds(60)).limitForPeriod(10).limitRefreshPeriod(Duration.ofSeconds(1)).build());

  public static final RateLimiter ONE_HUNDRED_PER_SECOND = RateLimiter.of("vertex-100-per-sec", RateLimiterConfig
      .custom().timeoutDuration(Duration.ofSeconds(60)).limitForPeriod(100).limitRefreshPeriod(Duration.ofSeconds(1)).build());

  private final AtomicLong reqCounter = new AtomicLong(1);
  private final String apiUrl;
  private final ExchangeSpecification exchangeSpecification;
  private final VertexStreamingExchange exchange;
  private final String customHost;
  private boolean authenticated;
  private final AtomicBoolean authenticating = new AtomicBoolean(false);
  private boolean wasAuthenticated;
  private Observable<JsonNode> allMessages;
  private final Map<Long, String> subscriptionIdToChannel = new java.util.concurrent.ConcurrentHashMap<>();
  private Disposable allMessageSub;

  public VertexStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification, VertexStreamingExchange exchange, RateLimiter subscriptionsRateLimiter, String name, String customHost) {
    super(apiUrl, MAX_FRAME_KB, Duration.ofSeconds(5), Duration.ofSeconds(1), 15, subscriptionsRateLimiter, name);
    this.apiUrl = apiUrl;
    this.exchangeSpecification = exchangeSpecification;
    this.exchange = exchange;
    this.customHost = customHost;
  }

  @Override
  protected DefaultHttpHeaders getCustomHeaders() {
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
    String subAccount = exchange.getSubAccountOrDefault();
    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);

    subscriptionIdToChannel.put(reqId, channelName);

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

  public void authenticate() {
    if (authenticated || !authenticating.compareAndSet(false, true)) return;
    try {
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
        LOG.info("Authentication response: {}", value);
        JsonNode idNode = value.get("id");
        if (idNode != null && idNode.asLong() == requestId) {
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
        LOG.info("Authentication successful: {}", response);
      } catch (InterruptedException e) {
        LOG.warn("Interrupted while waiting for authentication response");
        return;

      } catch (TimeoutException timeout) {
        // FIXME only happening on SEI connection
        LOG.warn("Timeout while waiting for authentication response, assuming we're authenticated", timeout);
      } catch (Throwable e) {
        throw new RuntimeException("Authentication error", e);
      } finally {
        responseSub.dispose();
      }
      wasAuthenticated = true;
      authenticated = true;
    } finally {
      authenticating.getAndSet(false);
    }
  }

  @Override
  public void resubscribeChannels() {
    authenticated = false;
    allMessages = subscribeChannel(ALL_MESSAGES).share();


    if (wasAuthenticated) {
      authenticate();
    }

    allMessageSub = allMessages.subscribe((message) -> {
      JsonNode idNode = message.get("id");
      if (idNode == null) return;
      String channelName = subscriptionIdToChannel.remove(idNode.asLong());
      if (channelName == null) {
        return;
      }
      LOG.debug("Subscription response: {}", message);
      if (message.get("error") != null) {
        LOG.error("Error subscribing to channel {}: {}", channelName, message.get("error"));
      } else {
        LOG.info("Subscribed to channel {} successfully", channelName);
      }
    });
    super.resubscribeChannels();

  }

  public Observable<JsonNode> allMessages() {
    if (allMessages == null) {
      throw new IllegalStateException("Not connected");
    }
    return allMessages;
  }

  @Override
  public Completable disconnect() {
    if (allMessageSub != null) {
      allMessageSub.dispose();
    }
    if (isSocketOpen()) {
      LOG.info("Disconnecting {}", apiUrl);
      return super.disconnect();
    } else {
      LOG.info("Already disconnected {}", apiUrl);
      return Completable.complete();
    }
  }
}
