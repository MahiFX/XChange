package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class VertexStreamingService extends JsonNettyStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(VertexStreamingService.class);

    //Channel to use to subscribe to all response
    public static final String ALL_MESSAGES = "all_messages";

    private final AtomicLong reqCounter = new AtomicLong(1);
    private final String apiUrl;

    public VertexStreamingService(String apiUrl) {
        super(apiUrl);
        this.apiUrl = apiUrl;
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) {
        JsonNode type = message.get("type");
        JsonNode productId = message.get("product_id");
        if (type != null) {
            if (productId != null) {
                return type.asText() + "." + productId.asText();
            }
            return type.asText();
        } else {
            return ALL_MESSAGES;
        }

    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) {
        if (Objects.equals(channelName, ALL_MESSAGES)) {
            return null;
        }
        String[] typeAndProduct = channelName.split("\\.");
        long reqId = reqCounter.incrementAndGet();
        return "{\n" +
                "  \"method\": \"subscribe\",\n" +
                "  \"stream\": {\n" +
                "    \"type\": \"" + typeAndProduct[0] + "\",\n" +
                "    \"product_id\": " + typeAndProduct[1] + "\n" +
                "  },\n" +
                "  \"id\": " + reqId + "\n" +
                "}";
    }

    @Override
    public String getUnsubscribeMessage(String channelName, Object... args) {
        if (Objects.equals(channelName, ALL_MESSAGES)) {
            return null;
        }
        String[] typeAndProduct = channelName.split("\\.");
        long reqId = reqCounter.incrementAndGet();
        return "{\n" +
                "  \"method\": \"unsubscribe\",\n" +
                "  \"stream\": {\n" +
                "    \"type\": \"" + typeAndProduct[0] + "\",\n" +
                "    \"product_id\": " + typeAndProduct[1] + "\n" +
                "  },\n" +
                "  \"id\": " + reqId + "\n" +
                "}";
    }


    public void sendUnsubscribeMessage(String channel) {
        sendMessage(getUnsubscribeMessage(channel));
    }

    public void sendSubscribeMessage(String channel) {
        sendMessage(getSubscribeMessage(channel));
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
