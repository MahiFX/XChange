package info.bitrich.xchangestream.binance;

import com.google.common.collect.Sets;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class test the Live Subscription/Unsubscription feature of the Binance Api. See
 * <a href="https://github.com/binance/binance-spot-api-docs/blob/master/web-socket-streams.md#live-subscribingunsubscribing-to-streams">...</a>
 *
 * <p>Before this addon, the subscription of the currency pairs required to be at the connection
 * time, so if we wanted to add new currencies to the stream, it was required to disconnect from the
 * stream and reconnect with the new ProductSubscription instance that contains all currency pairs.
 * With the new addon, we can subscribe to new currencies live without disconnecting the stream.
 */
public class BinanceMultipleDepthSubscriptionExample {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceMultipleDepthSubscriptionExample.class);

    public static void main(String[] args) throws InterruptedException {

        ExchangeSpecification spec =
                StreamingExchangeFactory.INSTANCE
                        .createExchange(BinanceStreamingExchange.class)
                        .getDefaultExchangeSpecification();
        BinanceStreamingExchange exchange =
                (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

        // We subscribe to 3 new currency pairs for trade (live subscription)
        // IMPORTANT!! Binance has a websocket limit of 5 incoming messages per second. If you bypass
        // this limit, the websocket will be disconnected.
        // (See
        // https://github.com/binance/binance-spot-api-docs/blob/master/web-socket-streams.md#websocket-limits for more details)
        // If you plan to subscribe/unsubscribe more than 5 currency pairs at a time, use a rate limiter
        // or keep the live subscription
        // feature disabled and connect your pairs at connection time only (default value).
        final Set<Instrument> currencyPairs =
                Sets.newHashSet(
                        CurrencyPair.BTC_USDT,
                        CurrencyPair.ETH_USDT,
                        new CurrencyPair("SOL/USDT"),
                        new CurrencyPair("CRV/USDT"),
                        new CurrencyPair("DYDX/USDT"),
                        new CurrencyPair("SEI/USDT"),
                        new CurrencyPair("BLUR/USDT"),
                        new CurrencyPair("NEAR/USDT"),
                        new CurrencyPair("WLD/USDT"),
                        CurrencyPair.XRP_USDT,
                        new CurrencyPair("AVAX/USDT"),
                        new CurrencyPair("DOGE/USDT"),
                        new CurrencyPair("INJ/USDT"),
                        new CurrencyPair("IMX/USDT"),
                        new CurrencyPair("GALA/USDT"),
                        new CurrencyPair("BCH/USDT"),
                        new CurrencyPair("JUP/USDT"),
                        new CurrencyPair("ATOM/USDT"),
                        new CurrencyPair("SNX/USDT"),
                        new CurrencyPair("STX/USDT"),
                        new CurrencyPair("BNB/USDT"),
                        new CurrencyPair("MKR/USDT"),
                        new CurrencyPair("ADA/USDT"),
                        CurrencyPair.LTC_USDT,
                        new CurrencyPair("ARB/USDT"),
                        new CurrencyPair("PEPE/USDT"),
                        new CurrencyPair("MATIC/USDT"),
                        new CurrencyPair("JTO/USDT"),
                        new CurrencyPair("OP/USDT"),
                        new CurrencyPair("FIL/USDT"),
                        new CurrencyPair("APE/USDT"),
                        new CurrencyPair("SUI/USDT"),
                        new CurrencyPair("ICP/USDT"),
                        new CurrencyPair("LINK/USDT"),
                        new CurrencyPair("LDO/USDT"),
                        new CurrencyPair("MEME/USDT"),
                        new CurrencyPair("TIA/USDT"),
                        new CurrencyPair("TRX/USDT"),
                        new CurrencyPair("PYTH/USDT"),
                        new CurrencyPair("APT/USDT"),
                        new CurrencyPair("DOT/USDT"),
                        new CurrencyPair("COMP/USDT")
                );
        final List<Disposable> disposableTrades = new ArrayList<>();

        ProductSubscription.ProductSubscriptionBuilder subscriptionBuilder = ProductSubscription.create();

        for (Instrument pair : currencyPairs) {
            subscriptionBuilder.addOrderbook(pair);
        }

        exchange.connect(subscriptionBuilder.build()).blockingAwait();

        LOG.info("Connected to the Exchange");

        for (final Instrument instrument : currencyPairs) {
            AtomicBoolean logBook = new AtomicBoolean(false);

            // Note: See the doOnDispose below. It's here that we will send an unsubscribe request to
            // Binance through the websocket instance.
            Disposable tradeDisposable =
                    exchange
                            .getStreamingMarketDataService()
                            .getOrderBook(instrument)
                            .subscribe(trade -> {
                                if (logBook.compareAndSet(false, true)) LOG.info("Book: {}", StringUtils.abbreviate(trade.toString(), 500));
                            });
            disposableTrades.add(tradeDisposable);
        }

        Thread.sleep(DateUtils.MILLIS_PER_DAY);

        // Now we unsubscribe BTC/USDT from the stream (TRADE and DEPTH) and also the another currency
        // pairs (TRADE 3x)
        // Note: we are ok with live unsubscription because we not bypass the limit of 5 messages per
        // second.
//    tradesBtc.dispose();
        disposableTrades.forEach(Disposable::dispose);

        LOG.info(
                "Now all symbols are live unsubscribed (BTC, ETH, LTC & XRP). We will live subscribe to XML/USDT and EOS/BTC...");
        Thread.sleep(5000);

        Disposable xlmDisposable =
                exchange
                        .getStreamingMarketDataService()
                        .getTrades((Instrument) CurrencyPair.XLM_USDT)
                        .doOnDispose(
                                () ->
                                        exchange
                                                .getStreamingMarketDataService()
                                                .unsubscribe(CurrencyPair.XLM_USDT, BinanceSubscriptionType.TRADE))
                        .subscribe(trade -> {
                        });
        Disposable eosDisposable =
                exchange
                        .getStreamingMarketDataService()
                        .getTrades((Instrument) CurrencyPair.EOS_BTC)
                        .doOnDispose(
                                () ->
                                        exchange
                                                .getStreamingMarketDataService()
                                                .unsubscribe(CurrencyPair.EOS_BTC, BinanceSubscriptionType.TRADE))
                        .subscribe(trade -> {
                        });

        Thread.sleep(5000);
        LOG.info("Test finished, we unsubscribe XML/USDT and EOS/BTC from the streams.");

        xlmDisposable.dispose();
        eosDisposable.dispose();

        exchange.disconnect().blockingAwait();
    }
}
