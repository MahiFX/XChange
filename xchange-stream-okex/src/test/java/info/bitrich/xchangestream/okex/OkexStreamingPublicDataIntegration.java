package info.bitrich.xchangestream.okex;

import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK400;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK5;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK50;
import static info.bitrich.xchangestream.okex.OkexStreamingService.ORDERBOOK_TOB;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.derivative.FuturesContract;
import org.knowm.xchange.instrument.Instrument;

public class OkexStreamingPublicDataIntegration {

  private StreamingExchange exchange;
  private final Instrument currencyPair = CurrencyPair.BTC_USDT;
  private final Instrument instrument = new FuturesContract("BTC/USDT/SWAP");

  @Before
  public void setUp() {
    exchange = StreamingExchangeFactory.INSTANCE.createExchange(OkexStreamingExchange.class);
    exchange.connect().blockingAwait();
  }

  @Test
  public void testTrades() throws InterruptedException {
    Disposable dis = exchange.getStreamingMarketDataService().getTrades(currencyPair)
        .subscribe(trade -> {
          System.out.println(trade);
          assertThat(trade.getInstrument()).isEqualTo(currencyPair);
        });
    Disposable dis2 = exchange.getStreamingMarketDataService().getTrades(instrument)
        .subscribe(trade -> {
          System.out.println(trade);
          assertThat(trade.getInstrument()).isEqualTo(instrument);
        });
    TimeUnit.SECONDS.sleep(3);
    dis.dispose();
    dis2.dispose();
  }

  @Test
  public void testTicker() throws InterruptedException {
    Disposable dis = exchange.getStreamingMarketDataService().getTicker(currencyPair)
        .subscribe(System.out::println);
    Disposable dis2 = exchange.getStreamingMarketDataService().getTicker(instrument)
        .subscribe(System.out::println);
    TimeUnit.SECONDS.sleep(3);
    dis.dispose();
    dis2.dispose();
  }

  @Test
  public void testFundingRateStream() throws InterruptedException {
    Disposable dis = exchange.getStreamingMarketDataService().getFundingRate(instrument)
        .subscribe(System.out::println);
    TimeUnit.SECONDS.sleep(3);
    dis.dispose();
  }

  @Test
  public void testOrderBookDefault() throws InterruptedException {
    AtomicBoolean ticked = new AtomicBoolean();
    AtomicBoolean ticked2 = new AtomicBoolean();
    Disposable dis = exchange.getStreamingMarketDataService().getOrderBook(currencyPair)
        .subscribe(orderBook -> {
          System.out.println(orderBook);
          assertThat(orderBook.getBids().get(0).getLimitPrice()).isLessThan(orderBook.getAsks().get(0).getLimitPrice());
          assertThat(orderBook.getBids().get(0).getInstrument()).isEqualTo(currencyPair);
          ticked.set(true);
        });
    Disposable dis2 = exchange.getStreamingMarketDataService().getOrderBook(instrument)
        .subscribe(orderBook -> {
          System.out.println(orderBook);
          assertThat(orderBook.getBids().get(0).getLimitPrice()).isLessThan(orderBook.getAsks().get(0).getLimitPrice());
          assertThat(orderBook.getBids().get(0).getInstrument()).isEqualTo(instrument);
          ticked2.set(true);
        });

    TimeUnit.SECONDS.sleep(3);

    assertTrue(ticked.get());
    assertTrue(ticked2.get());
    dis.dispose();
    dis2.dispose();
  }


  @Test
  public void testOrderBookTob() throws InterruptedException {
    assertChannelTicks(ORDERBOOK_TOB);
  }


  @Test
  public void testOrderBook() throws InterruptedException {
    assertChannelTicks(ORDERBOOK);
  }


  @Test
  @Ignore("Requires login with VIP4/5 access")
  public void testOrderBook50() throws InterruptedException {
    assertChannelTicks(ORDERBOOK50);
  }


  @Test
  @Ignore("Requires login with VIP4/5 access")
  public void testOrderBook400() throws InterruptedException {
    assertChannelTicks(ORDERBOOK400);
  }


  @Test
  public void testOrderBooks5() throws InterruptedException {
    assertChannelTicks(ORDERBOOK5);
  }

  private void assertChannelTicks(String channel) throws InterruptedException {
    AtomicBoolean ticked = new AtomicBoolean();
    AtomicBoolean ticked2 = new AtomicBoolean();
    Disposable dis = exchange.getStreamingMarketDataService().getOrderBook(currencyPair, channel)
        .subscribe(orderBook -> {
          System.out.println(orderBook);
          assertThat(orderBook.getBids().get(0).getLimitPrice()).isLessThan(orderBook.getAsks().get(0).getLimitPrice());
          assertThat(orderBook.getBids().get(0).getInstrument()).isEqualTo(currencyPair);
          ticked.set(true);
        });
    Disposable dis2 = exchange.getStreamingMarketDataService().getOrderBook(instrument, channel)
        .subscribe(orderBook -> {
          System.out.println(orderBook);
          assertThat(orderBook.getBids().get(0).getLimitPrice()).isLessThan(orderBook.getAsks().get(0).getLimitPrice());
          assertThat(orderBook.getBids().get(0).getInstrument()).isEqualTo(instrument);
          ticked2.set(true);
        });
    TimeUnit.SECONDS.sleep(5);

    assertTrue(ticked.get());
    assertTrue(ticked2.get());
    dis.dispose();
    dis2.dispose();
  }

}
