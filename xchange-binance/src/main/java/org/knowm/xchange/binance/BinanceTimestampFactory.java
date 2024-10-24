package org.knowm.xchange.binance;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.dto.meta.BinanceTime;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.client.ResilienceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.knowm.xchange.binance.BinanceResilience.REQUEST_WEIGHT_RATE_LIMITER;

public class BinanceTimestampFactory implements SynchronizedValueFactory<Long> {

  private static final Logger LOG = LoggerFactory.getLogger(BinanceTimestampFactory.class);
  private final ExchangeSpecification.ResilienceSpecification resilienceSpecification;
  private final ResilienceRegistries resilienceRegistries;

    private Long deltaServerTimeExpire;
    private Long deltaServerTime;

  public BinanceTimestampFactory(
      ExchangeSpecification.ResilienceSpecification resilienceSpecification,
      ResilienceRegistries resilienceRegistries) {
    this.resilienceSpecification = resilienceSpecification;
    this.resilienceRegistries = resilienceRegistries;
  }

    @Override
    public Long createValue() {

        return System.currentTimeMillis();
    }

    public void clearDeltaServerTime() {
        deltaServerTime = null;
    }

    public long deltaServerTime(BinanceTime binanceTime) throws IOException {

        if (deltaServerTime == null || deltaServerTimeExpire <= System.currentTimeMillis()) {

            // Do a little warm up
            Date serverTime = new Date(binanceTime(binanceTime).getServerTime().getTime());

            // Assume that we are closer to the server time when we get the repose
            Date systemTime = new Date(System.currentTimeMillis());

            // Expire every 10min
            deltaServerTimeExpire = systemTime.getTime() + TimeUnit.MINUTES.toMillis(10);
            deltaServerTime = serverTime.getTime() - systemTime.getTime();

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            LOG.trace(
                    "deltaServerTime: {} - {} => {}",
                    df.format(serverTime),
                    df.format(systemTime),
                    deltaServerTime);
        }

        return deltaServerTime;
    }

    private BinanceTime binanceTime(BinanceTime binanceTime) throws IOException {
        return ResilienceUtils.decorateApiCall(resilienceSpecification, () -> binanceTime)
                .withRetry(resilienceRegistries.retries().retry("time"))
                .withRateLimiter(
                        resilienceRegistries.rateLimiters().rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }
}
