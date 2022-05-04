package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import info.bitrich.xchangestream.deribit.dto.*;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.utils.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DeribitStreamingService extends JsonNettyStreamingService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingService.class);

    public static final String NO_CHANNEL_CHANNEL_NAME = "DERIBIT_NO_CHANNEL";

    private static final String HMAC_SHA256_ALGO = "HmacSHA256";
    private final ExchangeSpecification exchangeSpecification;
    private final long waitForNoChannelMessageMs;

    private final LoadingCache<Long, CompletableFuture<JsonNode>> noChannelMessageCache = CacheBuilder.newBuilder()
            .maximumSize(512)
            .build(new CacheLoader<Long, CompletableFuture<JsonNode>>() {
                @Override
                public CompletableFuture<JsonNode> load(Long key) {
                    return new CompletableFuture<>();
                }
            });

    private final AtomicLong messageCounter = new AtomicLong(0);

    public DeribitStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification) {
        super(apiUrl);
        this.exchangeSpecification = exchangeSpecification;
        Object messageResponseTimeout = exchangeSpecification.getExchangeSpecificParametersItem(DeribitStreamingExchange.MESSAGE_RESPONSE_TIMEOUT_OPTION);
        if (messageResponseTimeout instanceof Long) {
            waitForNoChannelMessageMs = (long) messageResponseTimeout;
        } else {
            waitForNoChannelMessageMs = 5000;
        }

        subscribeConnectionSuccess().subscribe(o -> {
            subscribeChannel(NO_CHANNEL_CHANNEL_NAME)
                    .forEach(json -> {
                        if (json.has("id")) {
                            Long id = json.get("id").asLong();

                            CompletableFuture<JsonNode> messageCompletableFuture = noChannelMessageCache.get(id);
                            messageCompletableFuture.complete(json);
                        }
                    });
        });

        startConnectionTester();
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) {
        if (message.has("params")) {
            JsonNode messageParams = message.get("params");

            if (messageParams.has("channel")) {
                return messageParams.get("channel").asText();
            }
        }

        return NO_CHANNEL_CHANNEL_NAME;
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
        if (NO_CHANNEL_CHANNEL_NAME.equals(channelName)) return "";

        DeribitBaseMessage<DeribitSubscribeParams> subscribeMessage = new DeribitBaseMessage<>("private/subscribe", new DeribitSubscribeParams(channelName));

        return objectMapper.writeValueAsString(subscribeMessage);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
        DeribitBaseMessage<DeribitSubscribeParams> unsubscribeMessage = new DeribitBaseMessage<>("public/unsubscribe", new DeribitSubscribeParams(channelName));

        return objectMapper.writeValueAsString(unsubscribeMessage);
    }

    @Override
    public void resubscribeChannels() {
        try {
            authenticate(exchangeSpecification.getApiKey(), exchangeSpecification.getSecretKey());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        super.resubscribeChannels();
    }

    public JsonNode waitForNoChannelMessage(long id) throws ExecutionException, InterruptedException, TimeoutException {
        return waitForNoChannelMessage(id, waitForNoChannelMessageMs);
    }

    public JsonNode waitForNoChannelMessage(long id, long timeoutMs) throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<JsonNode> pendingMessage = getNoChannelMessage(id);

        return pendingMessage.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<JsonNode> getNoChannelMessage(long id) throws ExecutionException {
        return noChannelMessageCache.get(id);
    }

    public void authenticate(String clientId, String clientSecret) throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGO);

        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        StringBuilder hmacKey = new StringBuilder();
        hmacKey.append(timestamp);
        hmacKey.append("\n");
        hmacKey.append(nonce);
        hmacKey.append("\n");

        String signatureData = hmacKey.toString();

        Mac mac = Mac.getInstance(HMAC_SHA256_ALGO);
        mac.init(secretKeySpec);

        String signature = DigestUtils.bytesToHex(mac.doFinal(signatureData.getBytes(StandardCharsets.UTF_8)));

        sendMessage(objectMapper.writeValueAsString(new DeribitBaseMessage<>("public/auth", new DeribitAuthParams(
                clientId,
                timestamp,
                signature,
                nonce,
                null
        ))));
    }

    public long getNextMessageId() {
        return messageCounter.incrementAndGet();
    }

    /**
     * Deribit connections seem to occasionally stop responding to messages, without disconnecting.
     * Periodically hit their API test endpoint to check the connection is live, and restart the connection if it appears dead.
     */
    private void startConnectionTester() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        AtomicInteger consecutiveFailures = new AtomicInteger(0);

        executor.scheduleAtFixedRate(() -> {
            if (isSocketOpen()) {
                long nextMessageId = getNextMessageId();
                try {
                    sendMessage(objectMapper.writeValueAsString(new DeribitBaseMessage<>(
                            nextMessageId,
                            "public/test",
                            null
                    )));
                } catch (IOException ignored) {
                    return;
                }

                try {
                    waitForNoChannelMessage(nextMessageId, 30_000L);
                } catch (TimeoutException timeout) {
                    int failures = consecutiveFailures.incrementAndGet();

                    if (failures >= 2) {
                        logger.error("Multiple timeouts waiting for response to API test call. Connection is considered dead - performing manual disconnect/reconnect.", timeout);
                        disconnect().blockingAwait();
                    }

                    return;
                } catch (ExecutionException | InterruptedException ignored) {
                    return;
                }

                consecutiveFailures.set(0);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
}
