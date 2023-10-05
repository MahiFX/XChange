package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class BinanceFuturesTopOfBookExample {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceFuturesTopOfBookExample.class);

    public static void main(String[] args) throws InterruptedException {
        ExchangeSpecification spec = StreamingExchangeFactory.INSTANCE
                .createExchange(BinanceFuturesStreamingExchange.class)
                .getDefaultExchangeSpecification();
        spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER, true);
        spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_REALTIME_TRADE, false);
        spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY, true);

        BinanceFuturesStreamingExchange exchange = (BinanceFuturesStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

        // First, we subscribe only for one currency pair at connection time (minimum requirement)
        ProductSubscription subscription =
                ProductSubscription.create()
                        .addTicker(CurrencyPair.BTC_USDT)
                        .addTrades(CurrencyPair.BTC_USDT)
                        .build();

        // Note: at connection time, the live subscription is disabled
        exchange.connect(subscription).blockingAwait();

        AtomicReference<Ticker> currentBook = new AtomicReference<>();

//        exchange
//                .getStreamingMarketDataService()
//                .getOrderBook(CurrencyPair.BTC_USDT)
//                .subscribe(
//                        orderBook -> {
////                            LOG.info("Top of Book: {} / {}", orderBook.getBids().stream().findFirst(), orderBook.getAsks().stream().findFirst());
//                            currentBook.set(orderBook);
//                        });

        exchange.getStreamingMarketDataService()
                .getTicker(CurrencyPair.BTC_USDT)
                .subscribe(currentBook::set);

        exchange
                .getStreamingMarketDataService()
                .getTrades(CurrencyPair.BTC_USDT)
                .subscribe(
                        trade -> {
                            Ticker tick = currentBook.get();

                            BigDecimal depth = null;
                            BigDecimal topOfbook = null;

                            if (tick != null) {
                                topOfbook = trade.getType() == Order.OrderType.BID ? tick.getBid() : tick.getAsk();

                                if (topOfbook != null) {
                                    depth = trade.getPrice().subtract(topOfbook);

                                    if (trade.getType() == Order.OrderType.BID) {
                                        depth = depth.negate();
                                    }
                                }
                            }

                            if (depth == null || depth.signum() >= 0) {
                                LOG.info("{} @ {} / ToB {} (depth={}): {} / {}", trade.getType(), trade.getPrice(), topOfbook, depth, trade, tick);
                            } else if (trade.getTimestamp().before(tick.getTimestamp())) {
                                LOG.error("**STALE TICK** {} @ {} / ToB {} (depth={}): {} / {}", trade.getType(), trade.getPrice(), topOfbook, depth, trade, tick);
                            } else {
                                LOG.error("**NEGATIVE DEPTH** {} @ {} / ToB {} (depth={}): {} / {}", trade.getType(), trade.getPrice(), topOfbook, depth, trade, tick);
                                LOG.error("**NEGATIVE DEPTH**", new IllegalStateException("Negative depth on current tick.\n\t" + trade + "\n\t" + tick));
                            }
                        });

        Thread.sleep(Long.MAX_VALUE);

    }
}
