package info.bitrich.xchangestream.cryptofacilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.cryptofacilities.dto.CryptoFacilitiesSubscriptionMessage;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesEventType;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesSubscriptionName;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import info.bitrich.xchangestream.service.netty.WebSocketClientCompressionAllowClientNoContextHandler;
import info.bitrich.xchangestream.service.netty.WebSocketClientHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author makarid, pchertalev
 */
public class CryptoFacilitiesStreamingService extends JsonNettyStreamingService {
    private static final Logger LOG = LoggerFactory.getLogger(CryptoFacilitiesStreamingService.class);
    private static final String EVENT = "event";
    private final Map<String, String> channels = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
    private final boolean isPrivate;
    private final Map<Integer, String> subscriptionRequestMap = new ConcurrentHashMap<>();

    public CryptoFacilitiesStreamingService(
            boolean isPrivate, String uri) {
        super(uri, Integer.MAX_VALUE);
        this.isPrivate = isPrivate;
    }

//    public CryptoFacilitiesStreamingService(
//            boolean isPrivate,
//            String uri,
//            int maxFramePayloadLength,
//            Duration connectionTimeout,
//            Duration retryDuration,
//            int idleTimeoutSeconds,
//            final Supplier<KrakenWebsocketToken> authData) {
//        super(uri, maxFramePayloadLength, connectionTimeout, retryDuration, idleTimeoutSeconds);
//        this.isPrivate = isPrivate;
//        this.authData = authData;
//    }

    @Override
    public boolean processArrayMessageSeparately() {
        return false;
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return WebSocketClientCompressionAllowClientNoContextHandler.INSTANCE;
    }

    @Override
    protected void handleMessage(JsonNode message) {
        String channelName;
        CryptoFacilitiesSubscriptionMessage subscriptionMessage;
        try {
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
                    case subscribed:
                        channelName = getChannel(message);
                        subscriptionMessage = mapper.treeToValue(message, CryptoFacilitiesSubscriptionMessage.class);
                        channels.put(subscriptionMessage.getProduct_ids().get(0), channelName);
                        break;
                    case unsubscribed:
                        channelName = getChannel(message);
                        LOG.info("Channel {} has been unsubscribed", channelName);
                        subscriptionMessage = mapper.treeToValue(message, CryptoFacilitiesSubscriptionMessage.class);
                        channels.remove(subscriptionMessage.getProduct_ids().get(0), channelName);
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
        } catch (JsonProcessingException e) {
            LOG.error("Error reading message: {}", e.getMessage(), e);
        }
        channelName = getChannel(message);
//        !message.isArray() ||
        if (channelName == null) {
            LOG.error("Unknown message: {}", message.toString());
            return;
        }

        super.handleMessage(message);
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
        String channelName = null;
        if (message.get("feed").asText().contains("book")) {
            if (message.has("product_id")) {
                channelName = "book" + CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER + message.get("product_id").asText();
            }
            if (message.has("product_ids")) {
                channelName = "book" + CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER + mapper.treeToValue(message.get("product_ids"), String[].class)[0].trim();
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("ChannelName {}", StringUtils.isBlank(channelName) ? "not defined" : channelName);
        }
        return channelName;
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
        String[] channelData =
                channelName.split(CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER);

        String pair = channelData[1];

        CryptoFacilitiesSubscriptionMessage subscriptionMessage =
                new CryptoFacilitiesSubscriptionMessage(
                        CryptoFacilitiesEventType.subscribe,
                        CryptoFacilitiesSubscriptionName.book,
                        Collections.singletonList(pair));
        return objectMapper.writeValueAsString(subscriptionMessage);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
        String[] channelData =
                channelName.split(CryptoFacilitiesStreamingMarketDataService.KRAKEN_CHANNEL_DELIMITER);

        String pair = channelData[1];
        CryptoFacilitiesSubscriptionMessage subscriptionMessage =
                new CryptoFacilitiesSubscriptionMessage(
                        CryptoFacilitiesEventType.unsubscribe,
                        CryptoFacilitiesSubscriptionName.book,
                        Collections.singletonList(pair));
        return objectMapper.writeValueAsString(subscriptionMessage);
    }

    @Override
    protected WebSocketClientHandler getWebSocketClientHandler(
            WebSocketClientHandshaker handshaker,
            WebSocketClientHandler.WebSocketMessageHandler handler) {
        LOG.info("Registering KrakenWebSocketClientHandler");
        return new KrakenWebSocketClientHandler(handshaker, handler);
    }

    private final WebSocketClientHandler.WebSocketMessageHandler channelInactiveHandler = null;

    /**
     * Custom client handler in order to execute an external, user-provided handler on channel events.
     * This is useful because it seems Kraken unexpectedly closes the web socket connection.
     */
    class KrakenWebSocketClientHandler extends NettyWebSocketClientHandler {

        public KrakenWebSocketClientHandler(
                WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler) {
            super(handshaker, handler);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            super.channelInactive(ctx);
            if (channelInactiveHandler != null) {
                channelInactiveHandler.onMessage("WebSocket Client disconnected!");
            }
        }
    }
}
