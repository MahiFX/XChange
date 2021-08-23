package info.bitrich.xchangestream.cryptofacilities;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.cryptofacilities.dto.enums.CryptoFacilitiesSubscriptionName;
import io.reactivex.Observable;
import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author makarid, pchertalev
 */
public class CryptoFacilitiesStreamingMarketDataService implements StreamingMarketDataService {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoFacilitiesStreamingMarketDataService.class);

    private static final int ORDER_BOOK_SIZE_DEFAULT = 25;
    private static final int[] KRAKEN_VALID_ORDER_BOOK_SIZES = {10, 25, 100, 500, 1000};
    private static final int MIN_DATA_ARRAY_SIZE = 4;

    public static final String KRAKEN_CHANNEL_DELIMITER = "-";

    private final CryptoFacilitiesStreamingService service;

    public CryptoFacilitiesStreamingMarketDataService(CryptoFacilitiesStreamingService service) {
        this.service = service;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        String channelName = getChannelName(CryptoFacilitiesSubscriptionName.book, currencyPair);
        int depth = parseOrderBookSize(args);
        Observable<ObjectNode> subscribe = subscribe(channelName, MIN_DATA_ARRAY_SIZE, depth);
        OrderbookSubscription orderbookSubscription = new OrderbookSubscription(subscribe);
        Observable<OrderBook> disconnectStream = service.subscribeDisconnect().map(
                o -> {
                    LOG.warn("Invalidating {} book due to disconnect {}", currencyPair, o);
                    orderbookSubscription.clear();
                    return orderbookSubscription.orderBook;
                }
        );

        Observable<OrderBook> orderBookStream = orderbookSubscription.stream
                .filter(objectNode -> {
                    int seqNumber = objectNode.get("seq").asInt();

                    if (orderbookSubscription.isValid(seqNumber))
                        return true;

                    LOG.warn("Message loss detected for " + currencyPair + ". lastSequenceNumber: " + orderbookSubscription.lastSequenceNumber + ", newSequence: " + seqNumber + " - clearing book");

                    orderbookSubscription.clear();

                    // TODO: Maybe restart the stream to force a snapshot if this happens frequently

                    return false;
                })
                .map(objectNode -> info.bitrich.xchangestream.cryptofacilities.CryptoFacilitiesStreamingAdapters.adaptFuturesOrderbookMessage(orderbookSubscription.orderBook, currencyPair, objectNode));

        return Observable.merge(
                orderBookStream,
                disconnectStream
        );
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        throw new UnsupportedOperationException("getTicker operation not currently supported");
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        String channelName = getChannelName(CryptoFacilitiesSubscriptionName.trade, currencyPair);

        Observable<ObjectNode> subscribe = subscribe(channelName, 1, null);

        return subscribe
                .flatMap(objectNode ->
                        Observable.fromIterable(CryptoFacilitiesStreamingAdapters.adaptTrades(currencyPair, objectNode))
                );
    }

    public Observable<ObjectNode> subscribe(String channelName, int maxItems, Integer depth) {
        return service
                .subscribeChannel(channelName, depth)
                .filter(node -> node instanceof ObjectNode)
                .map(node -> (ObjectNode) node)
                .filter(
                        list -> {
                            if (list.size() < maxItems) {
                                LOG.warn(
                                        "Invalid message in channel {}. It contains {} array items but expected at least {}",
                                        channelName,
                                        list.size(),
                                        maxItems);
                                return false;
                            }
                            return true;
                        });
    }

    public String getChannelName(CryptoFacilitiesSubscriptionName subscriptionName, CurrencyPair currencyPair) {
        String pair = currencyPair.base.toString(); // Use only base to match CryptoFacilitiesMarketDataServiceRaw.getCryptoFacilitiesOrderBook
        return subscriptionName + KRAKEN_CHANNEL_DELIMITER + pair;
    }

    private int parseOrderBookSize(Object[] args) {
        if (args != null && args.length > 0) {
            Object obSizeParam = args[0];
            LOG.debug("Specified Kraken order book size: {}", obSizeParam);
            if (Number.class.isAssignableFrom(obSizeParam.getClass())) {
                int obSize = ((Number) obSizeParam).intValue();
                if (ArrayUtils.contains(KRAKEN_VALID_ORDER_BOOK_SIZES, obSize)) {
                    return obSize;
                }
                LOG.error(
                        "Invalid order book size {}. Valid values: {}. Default order book size has been used: {}",
                        obSize,
                        ArrayUtils.toString(KRAKEN_VALID_ORDER_BOOK_SIZES),
                        ORDER_BOOK_SIZE_DEFAULT);
                return ORDER_BOOK_SIZE_DEFAULT;
            }
            LOG.error(
                    "Order book size param type {} is invalid. Expected: {}. Default order book size has been used {}",
                    obSizeParam.getClass().getName(),
                    Number.class,
                    ORDER_BOOK_SIZE_DEFAULT);
            return ORDER_BOOK_SIZE_DEFAULT;
        }

        LOG.debug(
                "Order book size param has not been specified. Default order book size has been used: {}",
                ORDER_BOOK_SIZE_DEFAULT);
        return ORDER_BOOK_SIZE_DEFAULT;
    }

    private static final class OrderbookSubscription {
        private final Observable<ObjectNode> stream;
        private int lastSequenceNumber = -1;

        private final OrderBook orderBook = new OrderBook(null, Lists.newArrayList(), Lists.newArrayList());

        private OrderbookSubscription(Observable<ObjectNode> stream) {
            this.stream = stream;
        }

        public boolean isValid(int nextSeq) {
            if (lastSequenceNumber == -1 || lastSequenceNumber + 1 == nextSeq) {
                lastSequenceNumber = nextSeq;
                return true;
            }

            return false;
        }

        public void clear() {
            orderBook.clear();
            lastSequenceNumber = -1;
        }
    }
}
