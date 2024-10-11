package org.knowm.xchange.coinbasepro.service.marketdata;

import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knowm.xchange.coinbasepro.CoinbaseProExchange.Parameters.PARAM_USE_PRIME;

public class TickerFetchIntegration {

  @Test
  public void tickerFetchTest() throws Exception {

    ExchangeSpecification exchangeSpecification = new ExchangeSpecification(CoinbaseProExchange.class);
    exchangeSpecification.setExchangeSpecificParametersItem(PARAM_USE_PRIME, true);
    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exchangeSpecification);
    MarketDataService marketDataService = exchange.getMarketDataService();
    Ticker ticker = marketDataService.getTicker(new CurrencyPair("BTC", "USD"));
    System.out.println(ticker.toString());
    assertThat(ticker).isNotNull();
  }
}
