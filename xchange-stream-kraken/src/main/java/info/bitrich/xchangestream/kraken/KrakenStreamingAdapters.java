package info.bitrich.xchangestream.kraken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.kraken.KrakenAdapters;
import org.knowm.xchange.kraken.dto.trade.KrakenType;
import org.knowm.xchange.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.bitrich.xchangestream.kraken.KrakenStreamingChecksum.createCrcChecksum;

/**
 * Kraken streaming adapters
 */
public class KrakenStreamingAdapters {
  private static final Logger LOG = LoggerFactory.getLogger(KrakenStreamingAdapters.class);

  static final String ASK_SNAPSHOT = "as";
  static final String ASK_UPDATE = "a";

  static final String BID_SNAPSHOT = "bs";
  static final String BID_UPDATE = "b";

  static final String CHECKSUM = "c";

  private static void updateInBook(
          int depth,
          Instrument instrument,
          Order.OrderType orderType,
          JsonNode currentNode,
          String key,
          TreeSet<LimitOrder> target) {
    adaptLimitOrders(instrument, orderType, currentNode.get(key))
            .forEachRemaining(
                    limitOrder -> {
                      target.removeIf(it -> it.getLimitPrice().compareTo(limitOrder.getLimitPrice()) == 0);
                      if (limitOrder.getOriginalAmount().compareTo(BigDecimal.ZERO) != 0) {
                        target.add(limitOrder);
                      }
                    });
    while (target.size() > depth) {
      LimitOrder last = target.last();
      target.remove(last);
    }
  }

  public static OrderBook adaptOrderbookMessage(
          int depth,
          TreeSet<LimitOrder> bids,
          TreeSet<LimitOrder> asks, Instrument instrument, ArrayNode arrayNode) {
    final AtomicLong expectedChecksum = new AtomicLong(0);
    final boolean awaitingSnapshot = (bids.isEmpty() && asks.isEmpty());
    arrayNode
            .elements()
            .forEachRemaining(
                    currentNode -> {
                      if (awaitingSnapshot) {
                        if (currentNode.has(BID_SNAPSHOT) && currentNode.has(ASK_SNAPSHOT)) {
                          LOG.info("Received {} snapshot, clearing book", instrument);
                          updateInBook(
                                  depth, instrument, Order.OrderType.BID, currentNode, BID_SNAPSHOT, bids);
                          updateInBook(
                                  depth, instrument, Order.OrderType.ASK, currentNode, ASK_SNAPSHOT, asks);
                        }
                      } else {
                        if (currentNode.has(BID_UPDATE)) {
                          updateInBook(
                                  depth, instrument, Order.OrderType.BID, currentNode, BID_UPDATE, bids);
                        }
                        if (currentNode.has(ASK_UPDATE)) {
                          updateInBook(
                                  depth, instrument, Order.OrderType.ASK, currentNode, ASK_UPDATE, asks);
                        }
                      }
              if (!awaitingSnapshot && currentNode.has(CHECKSUM)) {
                expectedChecksum.set(currentNode.get(CHECKSUM).asLong());
              }
            });
    if (bids.isEmpty() && asks.isEmpty()) {
      LOG.info("Ignoring {} message {}, awaiting snapshot", instrument, arrayNode);
    }
    long localChecksum = createCrcChecksum(asks, bids);
    if (expectedChecksum.get() > 0 && expectedChecksum.get() != localChecksum) {
      LOG.warn(
              "{} checksum does not match, expected {} but local checksum is {}",
              instrument,
              expectedChecksum.get(),
              localChecksum);
      throw new IllegalStateException("Checksum did not match");
    } else if (expectedChecksum.get() == 0) {
      LOG.debug("Skipping {} checksum validation, no expected checksum in message", instrument);
    } else if (bids.size() > 0
        && asks.size() > 0
        && bids.first().getLimitPrice().compareTo(asks.first().getLimitPrice()) >= 0) {
      throw new IllegalStateException(
              "CROSSED book "
                      + instrument
                      + " "
                      + bids.first().getLimitPrice()
                      + " >= "
                      + asks.first().getLimitPrice());
    }
    final Date lastTime =
            Stream.concat(asks.stream(), bids.stream())
                    .map(LimitOrder::getTimestamp)
                    .max(Date::compareTo)
                    .orElse(null);
    return new OrderBook(lastTime, Lists.newArrayList(asks),
            Lists.newArrayList(bids),
            true);
  }

  public static OrderBook adaptFuturesOrderbookMessage(
          OrderBook orderBook, Instrument instrument, ObjectNode objectNode) {
    if (objectNode.get("feed").asText().equals("book_snapshot")) {
      //Clear orderbook if receiving snapshot
      orderBook.clear();
      Date timestamp = new Date(objectNode.get("timestamp").asLong());
      adaptFuturesLimitOrders(instrument, Order.OrderType.BID, objectNode.withArray("bids"), null)
              .forEach(orderBook::update);
      adaptFuturesLimitOrders(instrument, Order.OrderType.ASK, objectNode.withArray("asks"), null)
              .forEach(orderBook::update);
    } else {
      if (objectNode.get("side").asText().equals("buy")) {
        orderBook.update(adaptFuturesLimitOrder(instrument, Order.OrderType.BID, objectNode));
      }
      if (objectNode.get("side").asText().equals("sell")) {
        orderBook.update(adaptFuturesLimitOrder(instrument, Order.OrderType.ASK, objectNode));
      }
    }
    return new OrderBook(
            orderBook.getTimeStamp(),
            Lists.newArrayList(orderBook.getAsks()),
            Lists.newArrayList(orderBook.getBids()),
            true);
  }

  private static void clearOrderbook(OrderBook orderBook){
    orderBook.getBids().clear();
    orderBook.getAsks().clear();
  }

  /**
   * Adapt a JsonNode to a Stream of limit orders, the node past in here should be the body of a
   * a/b/as/bs key.
   */
  public static Iterator<LimitOrder> adaptLimitOrders(
          Instrument instrument, Order.OrderType orderType, JsonNode node) {
    if (node == null || !node.isArray()) {
      return Collections.emptyIterator();
    }
    return Iterators.transform(
            node.elements(), jsonNode -> adaptLimitOrder(instrument, orderType, jsonNode));
  }

    public static Stream<LimitOrder> adaptFuturesLimitOrders(
            Instrument instrument, Order.OrderType orderType, ArrayNode node, Date timestamp) {
        if (node == null || !node.isArray()) {
            return Stream.empty();
        }
        return Streams.stream(node.elements())
//                .filter(JsonNode::isArray)
                .map(jsonNode -> adaptFututuresSnapshotLimitOrder(instrument, orderType, jsonNode, timestamp));
    }


    /**
     * Adapt a JsonNode containing two decimals into a LimitOrder
     */
    public static LimitOrder adaptLimitOrder(
            Instrument instrument, Order.OrderType orderType, JsonNode node) {
        if (node == null) {
            return null;
        }
        Iterator<JsonNode> iterator = node.elements();
        BigDecimal price = nextNodeAsDecimal(iterator);
        BigDecimal volume = nextNodeAsDecimal(iterator);
        Date timestamp = nextNodeAsDate(iterator);
        return new LimitOrder(orderType, volume, instrument, null, timestamp, price);
    }

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
    Date timestamp = DateUtils.fromUnixTime(node.get("timestamp").asLong());
    return new LimitOrder(orderType, volume, instrument, null, timestamp, price);
  }

  /**
   * Adapt an ArrayNode containing a ticker message into a Ticker
   */
  public static Ticker adaptTickerMessage(Instrument instrument, ArrayNode arrayNode) {
    return Streams.stream(arrayNode.elements())
            .filter(JsonNode::isObject)
            .map(
                    tickerNode -> {
                      ArrayNode askArray = (ArrayNode) tickerNode.get("a");
                      ArrayNode bidArray = (ArrayNode) tickerNode.get("b");
                      Iterator<JsonNode> closeIterator = tickerNode.get("c").iterator();
                      Iterator<JsonNode> volumeIterator = tickerNode.get("v").iterator();
                      Iterator<JsonNode> vwapIterator = tickerNode.get("p").iterator();
                      Iterator<JsonNode> lowPriceIterator = tickerNode.get("l").iterator();
                      Iterator<JsonNode> highPriceIterator = tickerNode.get("h").iterator();
                      Iterator<JsonNode> openPriceIterator = tickerNode.get("o").iterator();

                      // Move iterators forward here required, this ignores the first field if the desired
                      // value is in the second element.
                      vwapIterator.next();
                      volumeIterator.next();

                      return new Ticker.Builder()
                              .open(nextNodeAsDecimal(openPriceIterator))
                              .ask(arrayNodeItemAsDecimal(askArray, 0))
                              .bid(arrayNodeItemAsDecimal(bidArray, 0))
                              .askSize(arrayNodeItemAsDecimal(askArray, 2))
                              .bidSize(arrayNodeItemAsDecimal(bidArray, 2))
                              .last(nextNodeAsDecimal(closeIterator))
                              .high(nextNodeAsDecimal(highPriceIterator))
                              .low(nextNodeAsDecimal(lowPriceIterator))
                              .vwap(nextNodeAsDecimal(vwapIterator))
                              .volume(nextNodeAsDecimal(volumeIterator))
                              .instrument(instrument)
                              .build();
                    })
            .findFirst()
            .orElse(null);
  }

  public static Ticker adaptFuturesTickerMessage(Instrument instrument, ObjectNode objectNode) {
    return Streams.stream(objectNode.elements())
            .filter(JsonNode::isObject)
            .map(
                    tickerNode -> {
                      Iterator<JsonNode> askIterator = tickerNode.get("a").iterator();
                      Iterator<JsonNode> bidIterator = tickerNode.get("b").iterator();
                      Iterator<JsonNode> closeIterator = tickerNode.get("c").iterator();
                      Iterator<JsonNode> volumeIterator = tickerNode.get("v").iterator();
                      Iterator<JsonNode> vwapIterator = tickerNode.get("p").iterator();
                      Iterator<JsonNode> lowPriceIterator = tickerNode.get("l").iterator();
                      Iterator<JsonNode> highPriceIterator = tickerNode.get("h").iterator();
                      Iterator<JsonNode> openPriceIterator = tickerNode.get("o").iterator();

                      // Move iterators forward here required, this ignores the first field if the desired
                      // value is in the second element.
                      vwapIterator.next();
                      volumeIterator.next();

                      return new Ticker.Builder()
                              .open(nextNodeAsDecimal(openPriceIterator))
                              .ask(nextNodeAsDecimal(askIterator))
                              .bid(nextNodeAsDecimal(bidIterator))
                              .last(nextNodeAsDecimal(closeIterator))
                              .high(nextNodeAsDecimal(highPriceIterator))
                              .low(nextNodeAsDecimal(lowPriceIterator))
                              .vwap(nextNodeAsDecimal(vwapIterator))
                              .volume(nextNodeAsDecimal(volumeIterator))
                              .instrument(instrument)
                              .build();
                    })
            .findFirst()
            .orElse(null);
  }

  /**
   * Adapt an ArrayNode containing a spread message into a Ticker
   */
  public static Ticker adaptSpreadMessage(Instrument instrument, ArrayNode arrayNode) {
    ArrayNode data = (ArrayNode) arrayNode.get(1);

    return new Ticker.Builder()
            .ask(arrayNodeItemAsDecimal(data, 1))
            .bid(arrayNodeItemAsDecimal(data, 0))
            .askSize(arrayNodeItemAsDecimal(data, 4))
            .bidSize(arrayNodeItemAsDecimal(data, 3))
            .timestamp(DateUtils.fromMillisUtc((long) (Double.parseDouble(data.get(2).textValue()) * 1000)))
            .instrument(instrument)
            .build();
  }

  /**
   * Adapt an JsonNode into a list of Trade
   */
  public static List<Trade> adaptTrades(Instrument instrument, JsonNode arrayNode) {
    return Streams.stream(arrayNode.elements())
            .filter(JsonNode::isArray)
            .flatMap(
                    innerNode ->
                            Streams.stream(innerNode.elements())
                                    .map(inner -> KrakenStreamingAdapters.adaptTrade(instrument, inner)))
            .collect(Collectors.toList());
  }

  /**
   * Adapt an JsonNode into a single Trade
   */
  public static Trade adaptTrade(Instrument instrument, JsonNode arrayNode) {
    if (arrayNode == null || !arrayNode.isArray()) {
      return null;
    }
    Iterator<JsonNode> iterator = arrayNode.iterator();
    return new Trade.Builder()
            .price(nextNodeAsDecimal(iterator))
            .originalAmount(nextNodeAsDecimal(iterator))
            .timestamp(nextNodeAsDate(iterator))
            .type(nextNodeAsOrderType(iterator))
            .instrument(instrument)
            .build();
  }

  /**
   * Returns the element at index in arrayNode as a BigDecimal. Retuns null if the arrayNode is null
   * or index does not exist.
   */
  private static BigDecimal arrayNodeItemAsDecimal(ArrayNode arrayNode, int index) {
    if (arrayNode == null) {
      return null;
    }
    JsonNode itemNode = arrayNode.get(index);
    if (itemNode == null) {
      return null;
    }
    return new BigDecimal(itemNode.asText());
  }

  /**
   * Checks if a iterator has next node and returns the value as a BigDecimal. Returns null if the
   * iterator has no next value or the given iterator is null.
   */
  private static BigDecimal nextNodeAsDecimal(Iterator<JsonNode> iterator) {
    if (iterator == null || !iterator.hasNext()) {
      return null;
    }
    return new BigDecimal(iterator.next().textValue());
  }

  /**
   * Checks if a iterator has next node and returns the value as a Date using the long value as
   * timestamp. Returns null if the iterator has no next value or the given iterator is null.
   */
  private static Date nextNodeAsDate(Iterator<JsonNode> iterator) {
    if (iterator == null || !iterator.hasNext()) {
      return null;
    }
    return DateUtils.fromMillisUtc(
            (long) (Double.parseDouble(iterator.next().textValue()) * 1000));
  }

  /**
   * Checks if a iterator has next node and returns the value as a Date using the long value as
   * timestamp. Returns null if the iterator has no next value or the given iterator is null.
   */
  private static Order.OrderType nextNodeAsOrderType(Iterator<JsonNode> iterator) {
    if (iterator == null || !iterator.hasNext()) {
      return null;
    }
    return KrakenAdapters.adaptOrderType(KrakenType.fromString(iterator.next().textValue()));
    }
}
