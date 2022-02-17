package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import info.bitrich.xchangestream.deribit.dto.*;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.utils.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class DeribitStreamingService extends JsonNettyStreamingService {
    public static final String NO_CHANNEL_CHANNEL_NAME = "DERIBIT_NO_CHANNEL";
    public static final long WAIT_FOR_NO_CHANNEL_MESSAGE_MS = Long.getLong("DeribitStreamingService.WAIT_FOR_NO_CHANNEL_MESSAGE_MS", 2000);

    private static final String HMAC_SHA256_ALGO = "HmacSHA256";
    private final ExchangeSpecification exchangeSpecification;

    private final Cache<Long, Object> noChannelMessageCache = CacheBuilder.newBuilder().maximumSize(200).build();

    public DeribitStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification) {
        super(apiUrl);
        this.exchangeSpecification = exchangeSpecification;

        subscribeConnectionSuccess().subscribe(o -> {
            subscribeChannel(NO_CHANNEL_CHANNEL_NAME)
                    .forEach(json -> {

                        if (json.has("id")) {
                            Long id = json.get("id").asLong();

                            Object cached = noChannelMessageCache.getIfPresent(id);
                            noChannelMessageCache.put(id, json);

                            if (cached instanceof Semaphore) {
                                ((Semaphore) cached).release();
                            }
                        }
                    });
        });
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
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

    public JsonNode waitForNoChannelMessage(long id) throws ExecutionException, InterruptedException {
        return waitForNoChannelMessage(id, WAIT_FOR_NO_CHANNEL_MESSAGE_MS);
    }

    public JsonNode waitForNoChannelMessage(long id, long timeoutMs) throws ExecutionException, InterruptedException {
        Semaphore waitForMessage = new Semaphore(1);
        waitForMessage.acquire();

        Object result = noChannelMessageCache.get(id, () -> waitForMessage);

        if (result instanceof JsonNode) {
            return (JsonNode) result;
        } else {
            // RxJava thread will release the Semaphore on message arrival, allowing this acquire
            if (!waitForMessage.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) throw new RuntimeException("Didn't receive response with timeoutMs: " + timeoutMs);

            Object finalResult = noChannelMessageCache.getIfPresent(id);
            return (JsonNode) finalResult;
        }

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

}
