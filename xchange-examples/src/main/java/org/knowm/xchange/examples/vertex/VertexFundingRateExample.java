package org.knowm.xchange.examples.vertex;

import com.knowm.xchange.vertex.VertexProductInfo;
import com.knowm.xchange.vertex.VertexStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.instrument.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

    logger.info("Querying BTC-PERP funding rates");
    FundingRate fundingRate = exchange.getMarketDataService().getFundingRate(btc);
    logger.info("{} funding rate: {}% in {} minutes at {}", fundingRate.getInstrument(), fundingRate.getFundingRate1h().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP), fundingRate.getFundingRateEffectiveInMinutes(), fundingRate.getFundingRateDate());

    logger.info("Querying all rates");

    FundingRates fundingRates = exchange.getMarketDataService().getFundingRates();
    VertexStreamingExchange streamingExchange = (VertexStreamingExchange) exchange;
    VertexProductInfo productInfo = streamingExchange.getProductInfo();
    for (FundingRate rate : fundingRates.getFundingRates()) {
      Instrument instrument = rate.getInstrument();
      if (productInfo.isSpot(instrument) || productInfo.lookupProductId(instrument) == 0) {
        logger.info("{} rates: borrow {}%/deposit {}%", instrument, rate.getFundingRate1h().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP), rate.getFundingRate8h().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP));
      } else {
        logger.info("{} funding rate: {}% in {} minutes at {}.", instrument, rate.getFundingRate1h().multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP), rate.getFundingRateEffectiveInMinutes(), rate.getFundingRateDate());
      }
    }


    exchange.disconnect().blockingAwait();


  }

}
