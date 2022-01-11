package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.deribit.dto.DeribitSubscribeMessage;
import info.bitrich.xchangestream.deribit.dto.DeribitSubscribeParams;
import info.bitrich.xchangestream.deribit.dto.DeribitUnsubscribeMessage;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;

import java.io.IOException;

public class DeribitStreamingService extends JsonNettyStreamingService {
    public DeribitStreamingService(String apiUrl) {
        super(apiUrl);
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
}
