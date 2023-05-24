package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import org.knowm.xchange.ExchangeSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class VertexStreamingService extends JsonNettyStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(VertexStreamingService.class);

    private final AtomicLong reqCounter = new AtomicLong(1);

    public VertexStreamingService(String apiUrl, ExchangeSpecification exchangeSpecification) {
        super(apiUrl);
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
        JsonNode type = message.get("type");
        JsonNode productId = message.get("product_id");
        if (type != null) {
            if (productId != null) {
                return type.asText() + "." + productId.asText();
            }
            return type.asText();
        } else {
            logger.warn("Ignoring message with no type: {}", message);
            return null;
        }

    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
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
    public String getUnsubscribeMessage(String channelName, Object... args) throws IOException {
        Long productId = (Long) args[0];
        long reqId = reqCounter.incrementAndGet();
        return "{\n" +
                "  \"method\": \"unsubscribe\",\n" +
                "  \"stream\": {\n" +
                "    \"type\": \"" + channelName + "\",\n" +
                "    \"product_id\": " + productId + "\n" +
                "  },\n" +
                "  \"id\": " + reqId + "\n" +
                "}";
    }
}
