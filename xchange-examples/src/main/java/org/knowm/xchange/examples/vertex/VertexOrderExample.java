package org.knowm.xchange.examples.vertex;

import com.knowm.xchange.vertex.VertexOrderFlags;
import com.knowm.xchange.vertex.VertexStreamingExchange;
import com.knowm.xchange.vertex.VertexStreamingTradeService;
import com.knowm.xchange.vertex.dto.RewardsList;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.OpenPosition;
import org.knowm.xchange.dto.account.OpenPositions;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.params.CancelAllOrders;
import org.knowm.xchange.service.trade.params.DefaultCancelAllOrdersByInstrument;
import org.knowm.xchange.service.trade.params.DefaultCancelOrderByInstrumentAndIdParams;
import org.knowm.xchange.service.trade.params.orders.DefaultOpenOrdersParamInstrument;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class VertexOrderExample {

  private static final Logger log = LoggerFactory.getLogger(VertexOrderExample.class);


  public static void main(String[] args) throws IOException, InterruptedException {
    ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();

    ExchangeSpecification exchangeSpecification = StreamingExchangeFactory.INSTANCE
        .createExchangeWithoutSpecification(VertexStreamingExchange.class)
        .getDefaultExchangeSpecification();


    String walletPrivateKey = System.getProperty("WALLET_PRIVATE_KEY");
    if (StringUtils.isEmpty(walletPrivateKey)) {
      throw new IllegalArgumentException("Missing WALLET_PRIVATE_KEY env variable");
    }
    ECKeyPair ecKeyPair = Credentials.create(walletPrivateKey).getEcKeyPair();
    String address = "0x" + Keys.getAddress(ecKeyPair.getPublicKey());
    String subAccount = "default";

    exchangeSpecification.setApiKey(address);
    exchangeSpecification.setSecretKey(Numeric.toHexStringNoPrefix(ecKeyPair.getPrivateKey()));
    exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, true);
    exchangeSpecification.setExchangeSpecificParametersItem(VertexStreamingExchange.USE_LEVERAGE, true);
    exchangeSpecification.setExchangeSpecificParametersItem(VertexStreamingExchange.BLEND_LIQUIDATION_TRADES, true);
    exchangeSpecification.setUserName(subAccount); //subaccount name

    VertexStreamingExchange exchange = (VertexStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

    exchange.connect().blockingAwait();

    RewardsList rewardsList = exchange.queryRewards(address);

    VertexStreamingTradeService tradeService = exchange.getStreamingTradeService();


    CurrencyPair btc = new CurrencyPair("BTC-PERP", "USDC");

    Disposable trades = tradeService.getUserTrades(btc, subAccount).subscribe(userTrade -> {
      log.info("User trade: {}", userTrade);
    });

    Disposable changes = tradeService.getOrderChanges(btc, subAccount).subscribe(order -> {
      log.info("User order event: {}", order);
    });

    Thread.sleep(2000);

    OpenPositions initial = tradeService.getOpenPositions();
    log.info("Initial open positions: {}", initial);
    initial.getOpenPositions().forEach(openPosition -> {
      if (!openPosition.getInstrument().equals(btc)) return;
      MarketOrder closeOrder = new MarketOrder(openPosition.getType().equals(OpenPosition.Type.LONG) ? Order.OrderType.ASK : Order.OrderType.BID, openPosition.getSize().abs(), openPosition.getInstrument());
      try {
        log.info("Closing old position via " + closeOrder);
        String marketOrder = tradeService.placeMarketOrder(closeOrder);
        log.info("Closed position " + openPosition + " via order " + marketOrder);
      } catch (ExchangeException e) {
        log.error("Failed to close position " + openPosition);
      }

    });


    Thread.sleep(2000);

    OpenPositions openPositions = tradeService.getOpenPositions();
    assert openPositions.getOpenPositions().size() == 0;


    MarketOrder buy = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc);
    buy.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_IOC);
    tradeService.placeMarketOrder(buy);

    Thread.sleep(2000);

    log.info("Open positions before sell: {}", tradeService.getOpenPositions());

    MarketOrder sell = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), btc);
    sell.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_FOK);
    tradeService.placeMarketOrder(sell);

    LimitOrder resting = new LimitOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc, null, null, BigDecimal.valueOf(20000));
    String orderId = tradeService.placeLimitOrder(resting);

    //Test re-connect
    exchange.disconnect().blockingAwait(10, TimeUnit.SECONDS);

    Thread.sleep(1000);

    exchange.connect().blockingAwait();

    Thread.sleep(5000);

    log.info("Open orders before cancel: {}", tradeService.getOpenOrders(new DefaultOpenOrdersParamInstrument(btc)));
    log.info("Open positions before cancel: {}", tradeService.getOpenPositions());

    tradeService.cancelOrder(new DefaultCancelOrderByInstrumentAndIdParams(btc, orderId));

    log.info("Open orders after cancel: {}", tradeService.getOpenOrders(new DefaultOpenOrdersParamInstrument(btc)));

    // Check leveraged shorting works
    sell = new MarketOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), btc);
    sell.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_FOK);
    tradeService.placeMarketOrder(sell);

    buy = new MarketOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc);
    buy.addOrderFlag(VertexOrderFlags.TIME_IN_FORCE_IOC);
    tradeService.placeMarketOrder(buy);

    Thread.sleep(2000);

    LimitOrder resting2 = new LimitOrder(Order.OrderType.BID, BigDecimal.valueOf(0.01), btc, null, null, BigDecimal.valueOf(20000));
    String orderId2 = tradeService.placeLimitOrder(resting);


    log.info("Open orders before cancel all instrument: {}", tradeService.getOpenOrders());

    tradeService.cancelOrder(new DefaultCancelAllOrdersByInstrument(btc));

    log.info("Open orders after cancel: {}", tradeService.getOpenOrders(new DefaultOpenOrdersParamInstrument(btc)));


    LimitOrder resting3 = new LimitOrder(Order.OrderType.ASK, BigDecimal.valueOf(0.01), btc, null, null, BigDecimal.valueOf(40000));
    String orderId3 = tradeService.placeLimitOrder(resting);


    log.info("Open orders before cancel all instrument: {}", tradeService.getOpenOrders(new DefaultOpenOrdersParamInstrument(btc)));

    tradeService.cancelOrder(new CancelAllOrders() {
    });


    log.info("Open orders after cancel: {}", tradeService.getOpenOrders(new DefaultOpenOrdersParamInstrument(btc)));


    exchange.disconnect().blockingAwait();
  }
}
