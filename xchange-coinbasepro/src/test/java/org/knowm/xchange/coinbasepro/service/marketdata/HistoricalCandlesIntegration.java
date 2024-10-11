package org.knowm.xchange.coinbasepro.service.marketdata;

import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;
import org.knowm.xchange.coinbasepro.dto.marketdata.CoinbaseProCandle;
import org.knowm.xchange.coinbasepro.service.CoinbaseProMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knowm.xchange.coinbasepro.CoinbaseProExchange.Parameters.PARAM_USE_PRIME;

public class HistoricalCandlesIntegration {

  @Test
  public void tickerFetchTest() throws Exception {

    ExchangeSpecification exchangeSpecification = new ExchangeSpecification(CoinbaseProExchange.class);
    exchangeSpecification.setExchangeSpecificParametersItem(PARAM_USE_PRIME, true);
    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
    CoinbaseProMarketDataService mds =
        (CoinbaseProMarketDataService) exchange.getMarketDataService();
    CoinbaseProCandle[] candles =
        mds.getCoinbaseProHistoricalCandles(
            CurrencyPair.BTC_USD, "2018-02-01T00:00:00Z", "2018-02-01T00:10:00Z", "60");
    System.out.println(Arrays.toString(candles));
    assertThat(candles).hasSize(11);
  }
}
