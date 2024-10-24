package org.knowm.xchange.globitex;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.globitex.dto.marketdata.*;

import java.io.IOException;

@Path("/api/1/")
@Produces(MediaType.APPLICATION_JSON)
public interface Globitex {

  @GET
  @Path("public/symbols")
  GlobitexSymbols getSymbols() throws IOException;

  @GET
  @Path("public/ticker/{symbol}")
  GlobitexTicker getTickerBySymbol(@PathParam("symbol") String globitexSymbol) throws IOException;

  @GET
  @Path("public/ticker")
  GlobitexTickers getTickers() throws IOException;

  @GET
  @Path("public/orderbook/{symbol}")
  GlobitexOrderBook getOrderBookBySymbol(@PathParam("symbol") String globitexSymbol)
      throws IOException;

  @GET
  @Path("public/trades/recent/{symbol}")
  GlobitexTrades getRecentTradesBySymbol(
      @PathParam("symbol") String globitexSymbol,
      @QueryParam("maxResults") int maxResults,
      @QueryParam("formatItem") String formatItem,
      @QueryParam("side") boolean side)
      throws IOException;
}
