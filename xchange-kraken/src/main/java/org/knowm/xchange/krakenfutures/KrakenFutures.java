package org.knowm.xchange.krakenfutures;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.kraken.dto.marketdata.results.KrakenOrderBookResult;
import org.knowm.xchange.kraken.dto.marketdata.results.KrakenTickerResult;

import java.io.IOException;

/** @author Benedikt Bünz */
@Path("/api/v3/")
@Produces(MediaType.APPLICATION_JSON)
public interface KrakenFutures {

  @GET
  @Path("tickers")
  KrakenTickerResult getTicker(@QueryParam("pair") String currencyPairs) throws IOException;

  @GET
  @Path("orderbook")
  KrakenOrderBookResult getOrderbook(@QueryParam("symbol") String currencyPair) throws IOException;

}
