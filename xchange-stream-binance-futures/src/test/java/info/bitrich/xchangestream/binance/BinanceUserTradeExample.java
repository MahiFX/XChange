package info.bitrich.xchangestream.binance;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import org.apache.commons.lang3.ThreadUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinanceUserTradeExample {

  static final Logger logger = LoggerFactory.getLogger(BinanceUserTradeExample.class);

  public static void main(String[] args) throws IOException, InterruptedException {

    ExchangeSpecification spec = StreamingExchangeFactory.INSTANCE
        .createExchange(BinanceFuturesStreamingExchange.class)
        .getDefaultExchangeSpecification();
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_REALTIME_BOOK_TICKER, true);
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_HIGHER_UPDATE_FREQUENCY, true);
    spec.setExchangeSpecificParametersItem(BinanceStreamingExchange.USE_SANDBOX, true);
    spec.setApiKey(System.getProperty("API_KEY", "YOUR_API_KEY"));
    spec.setSecretKey(System.getProperty("SECRET_KEY", "YOUR_SECRET_KEY"));

    BinanceFuturesStreamingExchange exchange = (BinanceFuturesStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(spec);

    // First, we subscribe only for one currency pair at connection time (minimum requirement)
    ProductSubscription subscription =
        ProductSubscription.create()
            .addBalances(Currency.BTC)
            .addBalances(Currency.USDT)
            .addUserTrades(CurrencyPair.BTC_USDT)
            .build();

    // Note: at connection time, the live subscription is disabled
    exchange.connect(subscription).blockingAwait();

    exchange.getStreamingTradeService().getUserTrades(CurrencyPair.BTC_USDT)
        .forEach(t -> logger.info("Trade: " + t));

    exchange.getTradeService().placeMarketOrder(new MarketOrder.Builder(Order.OrderType.BID, CurrencyPair.BTC_USDT).originalAmount(BigDecimal.ONE).build());

    ThreadUtils.sleep(Duration.ofMillis(5000));

    exchange.getTradeService().placeMarketOrder(new MarketOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_USDT).originalAmount(BigDecimal.ONE).build());
  }
}
