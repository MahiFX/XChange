package info.bitrich.xchangestream.cryptofacilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.instrument.Instrument;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * Kraken streaming adapters
 */
public class CryptoFacilitiesStreamingAdapters {

    static final String ASK_SNAPSHOT = "asks";
    static final String ASK_UPDATE = "sell";

    static final String BID_SNAPSHOT = "bids";
    static final String BID_UPDATE = "buy";

    public static OrderBook adaptFuturesOrderbookMessage(
            OrderBook orderBook, Instrument instrument, ObjectNode objectNode) {
        if (objectNode.get("feed").asText().equals("book_snapshot")) {
            //Clear orderbook if receiving snapshot
            orderBook.clear();

            Date timestamp = new Date(objectNode.get("timestamp").asLong());
            adaptFuturesLimitOrders(instrument, Order.OrderType.BID, objectNode.withArray(BID_SNAPSHOT), timestamp)
                    .forEach(orderBook::update);
            adaptFuturesLimitOrders(instrument, Order.OrderType.ASK, objectNode.withArray(ASK_SNAPSHOT), timestamp)
                    .forEach(orderBook::update);
        } else {
            if (objectNode.get("side").asText().equals(BID_UPDATE)) {
                orderBook.update(adaptFuturesLimitOrder(instrument, Order.OrderType.BID, objectNode));
            }
            if (objectNode.get("side").asText().equals(ASK_UPDATE)) {
                orderBook.update(adaptFuturesLimitOrder(instrument, Order.OrderType.ASK, objectNode));
            }
        }
        return new OrderBook(
                orderBook.getTimeStamp(),
                Lists.newArrayList(orderBook.getAsks()),
                Lists.newArrayList(orderBook.getBids()),
                true);
    }

    /**
     * Adapt a JsonNode to a Stream of limit orders, the node past in here should be the body of a
     * a/b/as/bs key.
     */
    public static Stream<LimitOrder> adaptFuturesLimitOrders(
            Instrument instrument, Order.OrderType orderType, ArrayNode node, Date timestamp) {
        if (node == null || !node.isArray()) {
            return Stream.empty();
        }
        return Streams.stream(node.elements())
                .map(jsonNode -> adaptFututuresSnapshotLimitOrder(instrument, orderType, jsonNode, timestamp));
    }

    /**
     * Adapt a JsonNode containing two decimals into a LimitOrder
     */
    public static LimitOrder adaptFututuresSnapshotLimitOrder(
            Instrument instrument, Order.OrderType orderType, JsonNode node, Date timestamp) {
        if (node == null) {
            return null;
        }

        BigDecimal price = new BigDecimal(node.get("price").asText()).stripTrailingZeros();
        BigDecimal volume = new BigDecimal(node.get("qty").asText()).stripTrailingZeros();
        return new LimitOrder(orderType, volume, instrument, null, timestamp, price);
    }

    public static LimitOrder adaptFuturesLimitOrder(
            Instrument instrument, Order.OrderType orderType, ObjectNode node) {
        if (node == null) {
            return null;
        }
        BigDecimal price = new BigDecimal(node.get("price").asText()).stripTrailingZeros();
        BigDecimal volume = new BigDecimal(node.get("qty").asText()).stripTrailingZeros();
        Date timestamp = new Date(node.get("timestamp").asLong());
        return new LimitOrder(orderType, volume, instrument, null, timestamp, price);
    }

    public static List<Trade> adaptTrades(Instrument instrument, ObjectNode node) {
        if (node == null) return null;

        List<Trade> trades = new ArrayList<>();
        if (node.has("trades")) {
            ArrayNode jsonTrades = node.withArray("trades");

            for (JsonNode trade : jsonTrades) {
                trades.add(processTrade(instrument, trade));
            }
        } else {
            trades.add(processTrade(instrument, node));
        }

        return trades;
    }

    private static Trade processTrade(Instrument instrument, JsonNode tradeNode) {
        return new Trade.Builder()
                .type("sell".equals(tradeNode.get("side").textValue()) ? Order.OrderType.ASK : Order.OrderType.BID)
                .originalAmount(tradeNode.get("qty").decimalValue())
                .instrument(instrument)
                .price(tradeNode.get("price").decimalValue())
                .timestamp(new Date(tradeNode.get("time").asLong()))
                .id(tradeNode.get("uid").asText())
                .build();
    }

    public static Order.OrderType adaptOrderType(int direction) {
        switch (direction) {
            case 0:
                return Order.OrderType.BID;

            case 1:
                return Order.OrderType.ASK;

            default:
                throw new IllegalArgumentException("Unknown direction " + direction);
        }
    }
}
