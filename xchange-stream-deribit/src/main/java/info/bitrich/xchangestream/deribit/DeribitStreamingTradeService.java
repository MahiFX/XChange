package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.deribit.dto.*;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

public class DeribitStreamingTradeService implements StreamingTradeService, TradeService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingTradeService.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final DeribitStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
    private final AtomicLong messageCounter = new AtomicLong(0);

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
        DerebitOrderParams derebitOrderParams = new DerebitOrderParams(
                DeribitStreamingUtil.instrumentName(limitOrder.getInstrument()),
                limitOrder.getOriginalAmount(),
                limitOrder.getLimitPrice(),
                "limit",
                limitOrder.getUserReference(),
                getTimeInForce(limitOrder),
                limitOrder.hasFlag(DeribitOrderFlags.POST_ONLY));

        return sendDeribitOrderMessage(derebitOrderParams, DeribitStreamingUtil.getDirection(limitOrder.getType()));
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        DerebitOrderParams derebitOrderParams = new DerebitOrderParams(
                DeribitStreamingUtil.instrumentName(marketOrder.getInstrument()),
                marketOrder.getOriginalAmount(),
                null,
                "market",
                marketOrder.getUserReference(),
                getTimeInForce(marketOrder),
                marketOrder.hasFlag(DeribitOrderFlags.POST_ONLY));

        return sendDeribitOrderMessage(derebitOrderParams, DeribitStreamingUtil.getDirection(marketOrder.getType()));
    }

    private String sendDeribitOrderMessage(DerebitOrderParams derebitOrderParams, String direction) throws IOException {
        long messageId = messageCounter.incrementAndGet();
        DerebitOrderMessage derebitOrderMessage = new DerebitOrderMessage(derebitOrderParams, "private/" + direction, messageId);
        streamingService.sendMessage(mapper.writeValueAsString(derebitOrderMessage));

        JsonNode jsonNode;
        try {
            jsonNode = streamingService.waitForNoChannelMessage(messageId);
        } catch (Throwable t) {
            return null;
        }

        if (jsonNode != null) {
            if (jsonNode.has("result")) {
                JsonNode result = jsonNode.get("result");
                if (result.has("order")) {
                    JsonNode order = result.get("order");

                    if (order.has("order_id")) {
                        return order.get("order_id").asText();
                    }
                }
            }
        }

        return null;
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
