package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.deribit.dto.*;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import org.apache.commons.lang3.time.DateUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.account.OpenPositions;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.CancelOrderByIdParams;
import org.knowm.xchange.service.trade.params.CancelOrderParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DeribitStreamingTradeService implements StreamingTradeService, TradeService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingTradeService.class);

    public static final String VALID_UNTIL_MS_PROP = "Deribit.VALID_UNTIL_MS";

    private final DeribitStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingTradeService(DeribitStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
    }

    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        authenticate();

        String channelName = "user.orders." + DeribitStreamingUtil.instrumentName(currencyPair) + ".raw";

        logger.info("Subscribing to: " + channelName);

        return streamingService.subscribeChannel(channelName)
                .map(json -> {
                    DeribitOrderUpdate deribitOrderUpdate = DeribitStreamingUtil.tryGetDataAsType(mapper, json, DeribitOrderUpdate.class);

                    if (deribitOrderUpdate == null) {
                        return DeribitOrderUpdate.EMPTY;
                    } else {
                        return deribitOrderUpdate;
                    }
                })
                .filter(update -> update != DeribitOrderUpdate.EMPTY)
                .map(DeribitOrderUpdate::toOrder);
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        authenticate();

        String channelName = "user.trades." + DeribitStreamingUtil.instrumentName(currencyPair) + ".raw";

        logger.info("Subscribing to: " + channelName);

        return streamingService.subscribeChannel(channelName)
                .map(json -> {
                    DeribitUserTrade[] deribitUserTrade = DeribitStreamingUtil.tryGetDataAsType(mapper, json, DeribitUserTrade[].class);

                    if (deribitUserTrade == null) {
                        return new DeribitUserTrade[0];
                    } else {
                        return deribitUserTrade;
                    }
                })
                .flatMapIterable(dut -> {
                    List<UserTrade> userTrades = new ArrayList<>();

                    for (DeribitUserTrade deribitUserTrade : dut) {
                        userTrades.add(deribitUserTrade.toUserTrade());
                    }

                    return userTrades;
                });
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {
        DeribitOrderParams deribitOrderParams = orderToDeribitOrderParams(limitOrder, limitOrder.getLimitPrice(), "limit");

        return sendDeribitOrderMessage(deribitOrderParams, DeribitStreamingUtil.getDirection(limitOrder.getType()));
    }

    public CompletableFuture<String> placeLimitOrderAsync(LimitOrder limitOrder) throws IOException, ExecutionException, InterruptedException {
        DeribitOrderParams deribitOrderParams = orderToDeribitOrderParams(limitOrder, limitOrder.getLimitPrice(), "limit");

        return sendDeribitOrderMessageAsync(deribitOrderParams, DeribitStreamingUtil.getDirection(limitOrder.getType()));
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        DeribitOrderParams deribitOrderParams = orderToDeribitOrderParams(marketOrder, null, "market");

        return sendDeribitOrderMessage(deribitOrderParams, DeribitStreamingUtil.getDirection(marketOrder.getType()));
    }

    public CompletableFuture<String> placeMarketOrderAsync(MarketOrder marketOrder) throws IOException, ExecutionException, InterruptedException {
        DeribitOrderParams deribitOrderParams = orderToDeribitOrderParams(marketOrder, null, "market");

        return sendDeribitOrderMessageAsync(deribitOrderParams, DeribitStreamingUtil.getDirection(marketOrder.getType()));
    }

    private DeribitOrderParams orderToDeribitOrderParams(Order order, BigDecimal limitPrice, String type) {
        Integer validUntilMs = (Integer) exchangeSpecification.getExchangeSpecificParametersItem(VALID_UNTIL_MS_PROP);;

        return new DeribitOrderParams(
                DeribitStreamingUtil.instrumentName(order.getInstrument()),
                order.getOriginalAmount(),
                limitPrice,
                type,
                order.getUserReference(),
                getTimeInForce(order),
                order.hasFlag(DeribitOrderFlags.POST_ONLY),
                validUntilMs == null ? null : DateUtils.addMilliseconds(order.getTimestamp(), validUntilMs));
    }

    private String sendDeribitOrderMessage(DeribitOrderParams deribitOrderParams, String direction) throws IOException {
        long messageId = streamingService.getNextMessageId();
        DeribitBaseMessage<DeribitOrderParams> deribitOrderMessage = new DeribitBaseMessage<>(messageId, "private/" + direction, deribitOrderParams);
        streamingService.sendMessage(mapper.writeValueAsString(deribitOrderMessage));

        JsonNode jsonNode;
        try {
            jsonNode = streamingService.waitForNoChannelMessage(messageId);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to place new order: " + deribitOrderParams, t);
        }

        return getOrderIdFromOrderResult(jsonNode);
    }

    private CompletableFuture<String> sendDeribitOrderMessageAsync(DeribitOrderParams deribitOrderParams, String direction) throws IOException, ExecutionException, InterruptedException {
        long messageId = streamingService.getNextMessageId();
        DeribitBaseMessage<DeribitOrderParams> deribitOrderMessage = new DeribitBaseMessage<>(messageId, "private/" + direction, deribitOrderParams);
        streamingService.sendMessage(mapper.writeValueAsString(deribitOrderMessage));

        CompletableFuture<JsonNode> futureResult = streamingService.getNoChannelMessage(messageId);

       return futureResult.thenApply(this::getOrderIdFromOrderResult);
    }

    private String getOrderIdFromOrderResult(JsonNode jsonNode) {
        if (jsonNode != null) {
            if (jsonNode.has("result")) {
                JsonNode result = jsonNode.get("result");
                if (result.has("order")) {
                    JsonNode order = result.get("order");

                    if (order.has("order_id")) {
                        return order.get("order_id").asText();
                    }
                }
            } else if (jsonNode.has("error")) {
                JsonNode error = jsonNode.get("error");
                throw new ExchangeException("Error sending message: " + (error.has("message") ? error.get("message").asText() : "UNKNOWN_ERROR"));
            }
        }

        return null;
    }

    @Override
    public boolean cancelOrder(CancelOrderParams orderParams) throws IOException {
        if (orderParams instanceof CancelOrderByIdParams) {
            return cancelOrder(((CancelOrderByIdParams) orderParams).getOrderId());
        }

        throw new NotYetImplementedForExchangeException();
    }

    @Override
    public boolean cancelOrder(String orderId) throws IOException {
        long messageId = streamingService.getNextMessageId();
        streamingService.sendMessage(mapper.writeValueAsString(new DeribitBaseMessage<>(messageId, "private/cancel", new DeribitCancelOrderParams(orderId))));

        JsonNode jsonNode;

        try {
            jsonNode = streamingService.waitForNoChannelMessage(messageId);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to cancel order: " + orderId, t);
        }

        return processCancelResponse(jsonNode);
    }

    public CompletableFuture<Boolean> cancelOrderAsync(String orderId) throws ExecutionException, InterruptedException, IOException {
        long messageId = streamingService.getNextMessageId();
        streamingService.sendMessage(mapper.writeValueAsString(new DeribitBaseMessage<>(messageId, "private/cancel", new DeribitCancelOrderParams(orderId))));

        CompletableFuture<JsonNode> futureResult = streamingService.getNoChannelMessage(messageId);

        return futureResult.thenApply(this::processCancelResponse);
    }

    private boolean processCancelResponse(JsonNode jsonNode) {
        if (jsonNode != null) {
            if (jsonNode.has("result")) {
                JsonNode result = jsonNode.get("result");

                if (result.has("order_state")) {
                    String state = result.get("order_state").asText();

                    return "cancelled".equals(state) || "untriggered".equals(state);
                }
            }
        }

        return false;
    }

    public OpenPositions getOpenPositions(Currency currency) throws IOException {
        long messageId = streamingService.getNextMessageId();
        streamingService.sendMessage(mapper.writeValueAsString(new DeribitBaseMessage<>(messageId, "private/get_positions", new DeribitOpenPositionParams(currency.getCurrencyCode()))));

        JsonNode response;

        try {
            response = streamingService.waitForNoChannelMessage(messageId, 5_000);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get open positions.", t);
        }

        List<OpenPosition> positions = new ArrayList<>();
        if (response != null && response.has("result")) {
            JsonNode result = response.get("result");
            DeribitPosition[] deribitPositions = mapper.treeToValue(result, DeribitPosition[].class);

            for (DeribitPosition deribitPosition : deribitPositions) {
                positions.add(deribitPosition.toOpenPosition());
            }
        }

        return new OpenPositions(positions);
    }

    private DeribitTimeInForce getTimeInForce(Order order) {
        Set<Order.IOrderFlags> flags = order.getOrderFlags();

        for (Order.IOrderFlags flag : flags) {
            if (flag instanceof DeribitTimeInForce) return (DeribitTimeInForce) flag;
        }

        return null;
    }

    private void authenticate() {
        try {
            streamingService.authenticate(exchangeSpecification.getApiKey(), exchangeSpecification.getSecretKey());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
