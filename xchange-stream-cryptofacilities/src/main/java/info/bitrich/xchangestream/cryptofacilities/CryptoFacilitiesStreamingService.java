package info.bitrich.xchangestream.cryptofacilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesChallengeRequestMessage;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesSubscriptionMessage;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesSubscriptionName;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import info.bitrich.xchangestream.service.netty.WebSocketClientCompressionAllowClientNoContextHandler;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author makarid, pchertalev
 */
public class CryptoFacilitiesStreamingService extends JsonNettyStreamingService {
    private static final Logger LOG = LoggerFactory.getLogger(CryptoFacilitiesStreamingService.class);
    private static final String EVENT = "event";
    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
    private final String apiKey;
    private final CryptoFacilitiesHmacChallenge challengeSigner;
    private final AtomicReference<String[]> challenge = new AtomicReference<>();

    public CryptoFacilitiesStreamingService(String uri) {
        this(uri, null, null);
    }

    public CryptoFacilitiesStreamingService(String uri, String apiKey, String secretKey) {
        super(uri, Integer.MAX_VALUE);
        this.apiKey = apiKey;
        this.challengeSigner = secretKey != null ? new CryptoFacilitiesHmacChallenge(secretKey) : null;

        subscribeDisconnect().subscribe(
                o -> challenge.set(null),
                ignored -> {
                }
        );
    }

    @Override
    public boolean processArrayMessageSeparately() {
        return false;
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionAllowClientNoContextHandler.INSTANCE;
    }

    @Override
    public void resubscribeChannels() {
        if (apiKey == null) {
            super.resubscribeChannels();
        } else {
            sendChallengeRequest();
        }
    }

    private void sendChallengeRequest() {
        try {
            challenge.set(null);
            sendMessage(objectMapper.writeValueAsString(new CryptoFacilitiesChallengeRequestMessage(CryptoFacilitiesEventType.challenge, apiKey)));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to send challenge request", e);

        }
    }

    @Override
    protected void handleMessage(JsonNode message) {
        String channelName;
        JsonNode event = message.get(EVENT);
        CryptoFacilitiesEventType krakenEvent;
        if (event != null && (krakenEvent = CryptoFacilitiesEventType.getEvent(event.textValue())) != null) {
            switch (krakenEvent) {
                case info:
                    LOG.info("Info received: {}", message);
                    break;
                case pingStatus:
                    LOG.info("PingStatus received: {}", message);
                    break;
                case pong:
                    LOG.debug("Pong received");
                    break;
                case heartbeat:
                    LOG.debug("Heartbeat received");
                    break;
                case challenge:
                    challenge.set(signChallenge(message));
                    super.resubscribeChannels();
                    break;
                case subscribed:
                    channelName = getChannel(message);
                    LOG.info("Channel {} has been subscribed", channelName);
                    break;
                case unsubscribed:
                    channelName = getChannel(message);
                    LOG.info("Channel {} has been unsubscribed", channelName);
                    break;
                case subscribed_failed:
                case unsubscribed_failed:
                case error:
                case alert:
                    LOG.error(
                            "Error received: {}",
                            message.has("errorMessage")
                                    ? message.get("errorMessage").asText()
                                    : message.toString());
                    break;
                default:
                    LOG.warn("Unexpected event type has been received: {}", krakenEvent);
            }
            return;
        }
        channelName = getChannel(message);

        if (channelName == null) {
            LOG.error("Unknown message: {}", message);
            return;
        }

        super.handleMessage(message);
    }

    private String[] signChallenge(JsonNode message) {
        String challenge = message.get("message").asText();
        String signed = challengeSigner.signChallenge(challenge);
        return new String[]{challenge, signed};
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
        String channelName = null;
        String feed = message.get("feed").asText();

        if (feed.contains("book")) {
            channelName = productIdChannel("book", message);
        } else if (feed.startsWith("open_orders_verbose")) {
            channelName = CryptoFacilitiesSubscriptionName.open_orders_verbose.name();

        } else if (feed.startsWith("fills")) {
            channelName = CryptoFacilitiesSubscriptionName.fills.name();
        } else if (feed.startsWith("trade")) {
            channelName = productIdChannel("trade", message);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("ChannelName {}", StringUtils.isBlank(channelName) ? "not defined" : channelName);
        }
        return channelName;
    }

    private String productIdChannel(String baseChannelName, JsonNode message) throws IOException {
        String channelName = null;

        if (message.has("product_id")) {
            channelName = baseChannelName + CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER + message.get("product_id").asText();
        }
        if (message.has("product_ids")) {
            channelName = baseChannelName + CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER + mapper.treeToValue(message.get("product_ids"), String[].class)[0].trim();
        }

        return channelName;
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
        String[] challenge = null;

        if (apiKey != null) {
            challenge = this.challenge.get();

            if (challenge == null) {
                // Wait for response to resubscribeChannels() request
                LOG.info("Deferring subscription request for {} until challenge key received", channelName);
                return null;
            }
        }

        return getSubscriptionMessage(channelName, challenge, CryptoFacilitiesEventType.subscribe);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
        return getSubscriptionMessage(channelName, challenge.get(), CryptoFacilitiesEventType.unsubscribe);
    }

    private String getSubscriptionMessage(String channelName, String[] challenge, CryptoFacilitiesEventType unsubscribe) throws JsonProcessingException {
        String[] channelData = channelName.split(CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER);

        String pair = channelData.length > 1 ? channelData[1] : null;

        CryptoFacilitiesSubscriptionMessage subscriptionMessage =
                new CryptoFacilitiesSubscriptionMessage(
                        unsubscribe,
                        CryptoFacilitiesSubscriptionName.valueOf(channelData[0]),
                        pair != null ? Collections.singletonList(pair) : null,
                        challenge != null ? apiKey : null,
                        challenge != null ? challenge[0] : null,
                        challenge != null ? challenge[1] : null
                );
        return objectMapper.writeValueAsString(subscriptionMessage);
    }
}
