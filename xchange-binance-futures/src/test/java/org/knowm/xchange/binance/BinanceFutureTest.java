package org.knowm.xchange.binance;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.binance.futures.service.BinanceFuturesMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.params.DefaultCancelOrderByCurrencyPairAndIdParams;
import org.knowm.xchange.service.trade.params.orders.DefaultQueryOrderParamCurrencyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Ignore
public class BinanceFutureTest {

    private static final CurrencyPair instrument = new CurrencyPair("BTC/USDT");

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static Exchange binanceExchange;

    @Before
    public void setUp() throws IOException {
        Properties prop = new Properties();
        prop.load(this.getClass().getResourceAsStream("/secret.keys"));

        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(BinanceFuturesExchange.class);

        ExchangeSpecification spec = exchange.getExchangeSpecification();

        spec.setApiKey(prop.getProperty("apikey"));
        spec.setSecretKey(prop.getProperty("secret"));
        spec.setExchangeSpecificParametersItem(BinanceExchange.SPECIFIC_PARAM_USE_FUTURES_SANDBOX, true);
        exchange.applySpecification(spec);

        binanceExchange = exchange;
    }

    @Test
    public void binanceFutureMarketDataService() throws IOException {
        // Get OrderBook
        BinanceOrderbook orderBook = ((BinanceFuturesMarketDataService) binanceExchange.getMarketDataService()).getBinanceOrderbook(CurrencyPair.BTC_USDT, 10);
        logger.info("OrderBook: " + orderBook);
    }


    @Test
    public void binanceFutureTradeService() throws IOException {
        Set<Order.IOrderFlags> orderFlags = new HashSet<>();
        orderFlags.add(TimeInForce.GTX);
//        orderFlags.add(BinanceOrderFlags.LIMIT_MAKER);

        //Open Positions
        List<OpenPosition> openPositions = binanceExchange.getTradeService().getOpenPositions().getOpenPositions();
        logger.info("Positions: " + openPositions);
        assertThat(openPositions.stream().anyMatch(openPosition -> openPosition.getInstrument().equals(instrument))).isTrue();

//        // Get UserTrades
//        List<UserTrade> userTrades = binanceExchange.getTradeService().getTradeHistory(new BinanceTradeHistoryParams(instrument)).getUserTrades();
//        logger.info("UserTrades: "+ userTrades);
//        assertThat(userTrades.stream().anyMatch(userTrade -> userTrade.getInstrument().equals(instrument))).isTrue();

        // Place LimitOrder
        String orderId = binanceExchange.getTradeService().placeLimitOrder(new LimitOrder.Builder(Order.OrderType.BID, instrument)
                .limitPrice(BigDecimal.valueOf(1000))
                .flags(orderFlags)
                .originalAmount(BigDecimal.ONE)
                .build());
        // Get OpenOrders
        List<LimitOrder> openOrders = binanceExchange.getTradeService().getOpenOrders().getOpenOrders();
        logger.info("OpenOrders: " + openOrders);
        assertThat(openOrders.stream().anyMatch(openOrder -> openOrder.getInstrument().equals(instrument))).isTrue();

        // Get order
        Collection<Order> order = binanceExchange.getTradeService().getOrder(new DefaultQueryOrderParamCurrencyPair(instrument, orderId));
        logger.info("GetOrder: " + order);
        assertThat(order.stream().anyMatch(order1 -> order1.getInstrument().equals(instrument))).isTrue();

        //Cancel LimitOrder
        logger.info("CancelOrder: " + binanceExchange.getTradeService().cancelOrder(new DefaultCancelOrderByCurrencyPairAndIdParams(instrument, orderId)));
    }
}
