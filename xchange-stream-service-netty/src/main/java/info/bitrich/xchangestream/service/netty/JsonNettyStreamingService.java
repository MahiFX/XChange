package info.bitrich.xchangestream.service.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.io.IOException;
import java.time.Duration;

public abstract class JsonNettyStreamingService extends NettyStreamingService<JsonNode> {
  protected final ObjectMapper objectMapper = StreamingObjectMapperHelper.getObjectMapper();

  public JsonNettyStreamingService(String apiUrl) {
    super(apiUrl);
  }

  public JsonNettyStreamingService(String apiUrl, int maxFramePayloadLength) {
    super(apiUrl, maxFramePayloadLength);
  }

  public JsonNettyStreamingService(
      String apiUrl,
      int maxFramePayloadLength,
      Duration connectionTimeout,
      Duration retryDuration,
      int idleTimeoutSeconds) {
    super(apiUrl, maxFramePayloadLength, connectionTimeout, retryDuration, idleTimeoutSeconds, null);
  }


  public JsonNettyStreamingService(
      String apiUrl,
      int maxFramePayloadLength,
      Duration connectionTimeout,
      Duration retryDuration,
      int idleTimeoutSeconds,
      RateLimiter subscriptionsRateLimiter) {
    super(apiUrl, maxFramePayloadLength, connectionTimeout, retryDuration, idleTimeoutSeconds, subscriptionsRateLimiter);
  }

  public JsonNettyStreamingService(String apiUrl,
                                   int maxFramePayloadLength,
                                   Duration connectionTimeout,
                                   Duration retryDuration,
                                   int idleTimeoutSeconds,
                                   RateLimiter subscriptionsRateLimiter,
                                   String name) {
    super(apiUrl, maxFramePayloadLength, connectionTimeout, retryDuration, idleTimeoutSeconds, subscriptionsRateLimiter, name);
  }

  public boolean processArrayMessageSeparately() {
    return true;
  }

  @Override
  public void messageHandler(String message) {
    LOG.debug("Received message: {}", message);
    JsonNode jsonNode;

    // Parse incoming message to JSON
    try {
      jsonNode = objectMapper.readTree(message);
    } catch (IOException e) {
      LOG.error("Error parsing incoming message to JSON: {}", message);
      return;
    }

    if (processArrayMessageSeparately() && jsonNode.isArray()) {
      // In case of array - handle every message separately.
      for (JsonNode node : jsonNode) {
        handleMessage(node);
      }
    } else {
      handleMessage(jsonNode);
    }
  }

  protected void sendObjectMessage(Object message) {
    try {
      sendMessage(objectMapper.writeValueAsString(message));
    } catch (JsonProcessingException e) {
      LOG.error("Error creating json message: {}", e.getMessage());
    }
  }
}
