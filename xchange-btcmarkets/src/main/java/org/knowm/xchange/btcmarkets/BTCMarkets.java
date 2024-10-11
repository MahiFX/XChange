package org.knowm.xchange.btcmarkets;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsOrderBook;
import org.knowm.xchange.btcmarkets.dto.marketdata.BTCMarketsTicker;
import org.knowm.xchange.btcmarkets.dto.v3.marketdata.BTCMarketsTrade;

import java.io.IOException;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface BTCMarkets {

  @GET
  @Path("/market/{instrument}/{currency}/tick")
  BTCMarketsTicker getTicker(
      @PathParam("instrument") String instrument, @PathParam("currency") String currency)
      throws IOException;

  @GET
  @Path("/market/{instrument}/{currency}/orderbook")
  BTCMarketsOrderBook getOrderBook(
      @PathParam("instrument") String instrument, @PathParam("currency") String currency)
      throws IOException;

  @GET
  @Path("/v3/markets/{marketId}/trades")
  List<BTCMarketsTrade> getTrades(@PathParam("marketId") String marketId) throws IOException;

  @GET
  @Path("/v3/markets/{marketId}/trades")
  List<BTCMarketsTrade> getTrades(
      @QueryParam("before") Long before,
      @QueryParam("after") Long after,
      @QueryParam("limit") Integer limit,
      @PathParam("marketId") String marketId)
      throws IOException;
}
