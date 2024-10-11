package org.knowm.xchange.bybit;

import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bybit.service.BaseWiremockTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BybitExchangeTest extends BaseWiremockTest {


  @Test
  public void testSymbolLoading() throws IOException {
    Exchange bybitExchange = createExchange();

    stubFor(
        get(urlPathEqualTo("/v2/public/symbols"))
            .willReturn(
                aResponse()
                    .withStatus(Status.OK.getStatusCode())
                    .withHeader("Content-Type", "application/json")
                    .withBody(IOUtils.resourceToString("/getSymbols.json5", StandardCharsets.UTF_8))
            )
    );

    ExchangeSpecification specification = bybitExchange.getExchangeSpecification();
    specification.setShouldLoadRemoteMetaData(true);
    bybitExchange.applySpecification(specification);

    assertThat(bybitExchange.getExchangeMetaData().getInstruments()).hasSize(2);

  }
}
