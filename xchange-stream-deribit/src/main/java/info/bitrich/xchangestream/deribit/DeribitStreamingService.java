package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

public class DeribitStreamingService extends JsonNettyStreamingService {
    private static final String HMAC_SHA256_ALGO = "HmacSHA256";
    private final ExchangeSpecification exchangeSpecification;

    public DeribitStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification) {
        super(apiUrl);
        this.exchangeSpecification = exchangeSpecification;
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
        if (message.has("params")) {
            JsonNode messageParams = message.get("params");

            if (messageParams.has("channel")) {
                return messageParams.get("channel").asText();
            }
        }

        throw new IOException("Failed to read channel from message: " + message);
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
        DeribitSubscribeMessage subscribeMessage = new DeribitSubscribeMessage(new DeribitSubscribeParams(channelName));

        return objectMapper.writeValueAsString(subscribeMessage);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
        DeribitUnsubscribeMessage unsubscribeMessage = new DeribitUnsubscribeMessage(new DeribitSubscribeParams(channelName));

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

        sendMessage(objectMapper.writeValueAsString(new DeribitAuthMessage(new DeribitAuthParams(
                clientId,
                timestamp,
                signature,
                nonce,
                null
        ))));
    }

}
