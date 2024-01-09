package org.knowm.xchange.examples.vertex;

import com.knowm.xchange.vertex.VertexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.apache.commons.lang3.ThreadUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public class VertexFundingRateExample {

  private static final Logger logger = LoggerFactory.getLogger(VertexFundingRateExample.class);


  public static void main(String[] args) throws InterruptedException, IOException {
    ExchangeSpecification exchangeSpecification = new ExchangeSpecification(VertexStreamingExchange.class);

    String privateKey = System.getProperty("WALLET_PRIVATE_KEY");
    ECKeyPair ecKeyPair = Credentials.create(privateKey).getEcKeyPair();
    String address = "0x" + Keys.getAddress(ecKeyPair.getPublicKey());

    exchangeSpecification.setApiKey(address);
    exchangeSpecification.setSecretKey(privateKey);

    exchangeSpecification.setExchangeSpecificParametersItem(StreamingExchange.USE_SANDBOX, false);

    StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(exchangeSpecification);

    exchange.connect().blockingAwait();

    CurrencyPair btc = new CurrencyPair("BTC-PERP", "USDC");

    for (int i = 0; i < 60; i++) {
      FundingRate fundingRate = exchange.getMarketDataService().getFundingRate(btc);
      logger.info("Funding rate: {}% in {} minutes at {}", fundingRate.getFundingRate1h().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP), fundingRate.getFundingRateEffectiveInMinutes(), fundingRate.getFundingRateDate());
      ThreadUtils.sleep(Duration.ofSeconds(5));
    }


    exchange.disconnect().blockingAwait();


  }

}
