package org.knowm.xchange.btcc;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.btcc.dto.marketdata.BTCCTicker;

import java.io.IOException;
import java.util.Map;

@Path("data/pro/")
@Produces(MediaType.APPLICATION_JSON)
public interface BTCC {

  @GET
  @Path("ticker?symbol={symbol}")
  Map<String, BTCCTicker> getMarketTicker(@PathParam("symbol") String symbol) throws IOException;
}
