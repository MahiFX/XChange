package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.SignatureAndDigest;
import com.knowm.xchange.vertex.signing.schemas.StreamAuthentication;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.ExchangeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.knowm.xchange.vertex.dto.VertexModelUtils.buildSender;

public class VertexStreamingService extends JsonNettyStreamingService {

  private static final Logger logger = LoggerFactory.getLogger(VertexStreamingService.class);

  //Channel to use to subscribe to all response
  public static final String ALL_MESSAGES = "all_messages";

  private final AtomicLong reqCounter = new AtomicLong(1);
  private final String apiUrl;
  private final ExchangeSpecification exchangeSpecification;
  private final VertexStreamingExchange exchange;
  private boolean authenticated;
  private Observable<JsonNode> allMessages;

  public VertexStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification, VertexStreamingExchange exchange) {
    super(apiUrl);
    this.apiUrl = apiUrl;
    this.exchangeSpecification = exchangeSpecification;
    this.exchange = exchange;
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
    AtomicReference<Disposable> responseSub = new AtomicReference<>();

    responseSub.set(allMessages.subscribe((message) -> {
      logger.debug("Subscription response: {}", message);
      if (message.get("id").asLong() == reqId) {
        if (message.get("error") != null) {
          logger.error("Error subscribing to channel " + channelName + ": " + message.get("error"));
        } else {
          logger.info("Subscribed to channel " + channelName + " successfully");
        }
        responseSub.get().dispose();
      }
    }));

    return "{\n" +
        "  \"method\": \"subscribe\",\n" +
        "  \"stream\": {\n" +
        "    \"type\": \"" + typeAndProduct[0] + "\"\n" +
        productIdField(typeAndProduct) +
        subAccountField(typeAndProduct) +
        "  },\n" +
        "  \"id\": " + reqId + "\n" +
        "}";
  }

  private static String productIdField(String[] typeAndProduct) {
    return typeAndProduct.length > 1 ? ", \"product_id\": " + typeAndProduct[1] + "\n" : "";
  }

  private String subAccountField(String[] typeAndProduct) {
    if (typeAndProduct.length > 2) {
      return ",\"subaccount\": \"" + typeAndProduct[2] + "\"\n";
    } else {
      return "";
    }
  }

  @Override
  public String getUnsubscribeMessage(String channelName, Object... args) {
    if (channelName.startsWith(ALL_MESSAGES)) {
      return null;
    }
    String[] typeAndProduct = channelName.split("\\.");
    long reqId = reqCounter.incrementAndGet();
    return "{\n" +
        "  \"method\": \"unsubscribe\",\n" +
        "  \"stream\": {\n" +
        "    \"type\": \"" + typeAndProduct[0] + "\"\n" +
        productIdField(typeAndProduct) +
        subAccountField(typeAndProduct) +
        "  },\n" +
        "  \"id\": " + reqId + "\n" +
        "}";
  }

  public void authenticate() {

    String subAccount = exchange.getSubAccountOrDefault();

    String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);

    Instant expiry = Instant.now().plus(10, ChronoUnit.SECONDS);
    String timestamp = String.valueOf(expiry.toEpochMilli());

    long chainId = exchange.getChainId();
    String endpointContract = exchange.getEndpointContract();

    if (chainId == 0 || endpointContract == null) {
      throw new IllegalStateException("ChainId or EndpointContract not available. Cannot authenticate");
    }

    StreamAuthentication streamAuth = StreamAuthentication.build(chainId,
        endpointContract,
        sender,
        BigInteger.valueOf(expiry.toEpochMilli()));
    SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(streamAuth);

    logger.info("Authenticating stream");

    CompletableFuture<JsonNode> responseLatch = new CompletableFuture<>();
    long requestId = reqCounter.incrementAndGet();
    Disposable responseSub = allMessages.subscribe(value -> {
      logger.debug("Authentication response: {}", value);
      if (value.get("id").asLong() == requestId) {
        responseLatch.complete(value);
      }
      if (value.get("error") != null) {
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

      JsonNode response = responseLatch.get(5, TimeUnit.SECONDS);
      if (response.get("error") != null) {
        throw new RuntimeException("Authentication error: " + response.get("error"));
      }
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for authentication response");
      return;

    } catch (TimeoutException | ExecutionException e) {
      throw new RuntimeException("Authentication timeout");
    } finally {
      responseSub.dispose();
    }
    authenticated = true;
  }

  @Override
  public void resubscribeChannels() {
    if (authenticated) {
      authenticate();
    }
    allMessages = subscribeChannel(ALL_MESSAGES).share();
    super.resubscribeChannels();
  }

  @Override
  public Completable disconnect() {
    if (isSocketOpen()) {
      logger.info("Disconnecting " + apiUrl);
      return super.disconnect();
    } else {
      logger.info("Already disconnected " + apiUrl);
      return Completable.complete();
    }
  }
}
