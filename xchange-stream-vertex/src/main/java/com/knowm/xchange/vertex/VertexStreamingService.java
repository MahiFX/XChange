package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class VertexStreamingService extends JsonNettyStreamingService {

    //Channel to use to subscribe to all response
    public static final String ALL_MESSAGES = "all_messages";

    private final AtomicLong reqCounter = new AtomicLong(1);

    public VertexStreamingService(String apiUrl) {
        super(apiUrl);
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
}
