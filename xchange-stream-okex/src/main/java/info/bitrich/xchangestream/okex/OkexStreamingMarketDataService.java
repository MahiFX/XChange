package info.bitrich.xchangestream.okex;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import static info.bitrich.xchangestream.okex.OkexStreamingService.FUNDING_RATE;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK;
import static info.bitrich.xchangestream.okex.OkexStreamingService.TICKERS;
import static info.bitrich.xchangestream.okex.OkexStreamingService.TRADES;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.OrderBookUpdate;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.okex.OkexAdapters;
import org.knowm.xchange.okex.dto.marketdata.OkexFundingRate;
import org.knowm.xchange.okex.dto.marketdata.OkexOrderbook;
import org.knowm.xchange.okex.dto.marketdata.OkexPublicOrder;
import org.knowm.xchange.okex.dto.marketdata.OkexTicker;
import org.knowm.xchange.okex.dto.marketdata.OkexTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkexStreamingMarketDataService implements StreamingMarketDataService {

  private static final Logger LOG = LoggerFactory.getLogger(OkexStreamingMarketDataService.class);

  private final OkexStreamingService service;

  private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();
  private final Map<Instrument, PublishSubject<List<OrderBookUpdate>>> orderBookUpdatesSubscriptions;

  public OkexStreamingMarketDataService(OkexStreamingService service) {
    this.service = service;
    this.orderBookUpdatesSubscriptions = new ConcurrentHashMap<>();
  }

  private final Map<String, OrderBook> orderBookMap = new HashMap<>();

  @Override
  public Observable<Ticker> getTicker(Instrument instrument, Object... args) {
    String channelUniqueId = TICKERS + ":" + OkexAdapters.adaptInstrument(instrument);

    return service
        .subscribeChannel(channelUniqueId)
        .filter(message -> message.has("data"))
        .flatMap(
            jsonNode -> {
              List<OkexTicker> okexTickers =
                  mapper.treeToValue(
                      jsonNode.get("data"),
                      mapper
                          .getTypeFactory()
                          .constructCollectionType(List.class, OkexTicker.class));
              return Observable.fromIterable(okexTickers).map(OkexAdapters::adaptTicker);
            });
  }

  @Override
  public Observable<Trade> getTrades(Instrument instrument, Object... args) {
    String channelUniqueId = TRADES + ":" + OkexAdapters.adaptInstrument(instrument);

    return service
        .subscribeChannel(channelUniqueId)
        .filter(message -> message.has("data"))
        .flatMap(
            jsonNode -> {
              List<OkexTrade> okexTradeList =
                  mapper.treeToValue(
                      jsonNode.get("data"),
                      mapper.getTypeFactory().constructCollectionType(List.class, OkexTrade.class));
              return Observable.fromIterable(
                  OkexAdapters.adaptTrades(okexTradeList, instrument).getTrades());
            });
  }

  @Override
  public Observable<FundingRate> getFundingRate(Instrument instrument, Object... args) {
    String channelUniqueId = FUNDING_RATE + ":" + OkexAdapters.adaptInstrument(instrument);

    return service
        .subscribeChannel(channelUniqueId)
        .filter(message -> message.has("data"))
        .map(
            jsonNode -> {
              List<OkexFundingRate> okexFundingRates =
                  mapper.treeToValue(
                      jsonNode.get("data"),
                      mapper
                          .getTypeFactory()
                          .constructCollectionType(List.class, OkexFundingRate.class));
              return OkexAdapters.adaptFundingRate(okexFundingRates);
            });
  }

  @Override
  public Observable<OrderBook> getOrderBook(Instrument instrument, Object... args) {
    String instId = OkexAdapters.adaptInstrument(instrument);
    String channelName = args.length >= 1 ? args[0].toString() : ORDERBOOK;
    String channelUniqueId = channelName + ":" + instId;

    return service
        .subscribeChannel(channelUniqueId)
        .filter(jsonNode -> !jsonNode.has("event")) // ignore subscription r
        .flatMap(
            jsonNode -> {
              String action = jsonNode.has("action") ? jsonNode.get("action").asText() : "snapshot";
              if ("snapshot".equalsIgnoreCase(action)) {
                List<OkexOrderbook> okexOrderbooks =
                    mapper.treeToValue(
                        jsonNode.get("data"),
                        mapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, OkexOrderbook.class));
                OrderBook orderBook = OkexAdapters.adaptOrderBook(okexOrderbooks, instrument);
                orderBookMap.put(instId, orderBook);
                return Observable.just(orderBook);
              } else if ("update".equalsIgnoreCase(action)) {
                OrderBook orderBook = orderBookMap.getOrDefault(instId, null);
                if (orderBook == null) {
                  LOG.error(String.format("Failed to get orderBook, instId=%s.", instId));
                  return Observable.fromIterable(new LinkedList<>());
                }
                List<OkexPublicOrder> asks =
                    mapper.treeToValue(
                        jsonNode.get("data").get(0).get("asks"),
                        mapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, OkexPublicOrder.class));
                asks.forEach(
                    okexPublicOrder ->
                        orderBook.update(
                            OkexAdapters.adaptLimitOrder(
                                okexPublicOrder, instrument, Order.OrderType.ASK)));

                List<OkexPublicOrder> bids =
                    mapper.treeToValue(
                        jsonNode.get("data").get(0).get("bids"),
                        mapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, OkexPublicOrder.class));
                bids.forEach(
                    okexPublicOrder ->
                        orderBook.update(
                            OkexAdapters.adaptLimitOrder(
                                okexPublicOrder, instrument, Order.OrderType.BID)));
                if (orderBookUpdatesSubscriptions.get(instrument) != null) {
                  orderBookUpdatesSubscriptions(
                      instrument,
                      asks,
                      bids,
                      new Timestamp(
                          Long.parseLong(jsonNode.get("data").get(0).get("ts").asText())));
                }
                return Observable.just(orderBook);

              } else {
                LOG.error(
                    String.format("Unexpected books action=%s, message=%s", action, jsonNode));
                return Observable.fromIterable(new LinkedList<>());
              }
            });
  }

  public Observable<List<OrderBookUpdate>> getOrderBookUpdates(Instrument instrument) {
    return orderBookUpdatesSubscriptions.computeIfAbsent(instrument, v -> PublishSubject.create());
  }

  private void orderBookUpdatesSubscriptions(
      Instrument instrument, List<OkexPublicOrder> asks, List<OkexPublicOrder> bids, Date date) {
    List<OrderBookUpdate> orderBookUpdates = new ArrayList<>();
    for (OkexPublicOrder ask : asks) {
      OrderBookUpdate o =
          new OrderBookUpdate(
              Order.OrderType.ASK,
              ask.getVolume(),
              instrument,
              ask.getPrice(),
              date,
              ask.getVolume());
      orderBookUpdates.add(o);
    }
    for (OkexPublicOrder bid : bids) {
      OrderBookUpdate o =
          new OrderBookUpdate(
              Order.OrderType.BID,
              bid.getVolume(),
              instrument,
              bid.getPrice(),
              date,
              bid.getVolume());
      orderBookUpdates.add(o);
    }
    orderBookUpdatesSubscriptions.get(instrument).onNext(orderBookUpdates);
  }

}
