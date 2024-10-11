package org.knowm.xchange.bitfinex.v1;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.bitfinex.v1.dto.BitfinexExceptionV1;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Path("v1")
@Produces(MediaType.APPLICATION_JSON)
public interface Bitfinex {

  @GET
  @Path("pubticker/{symbol}")
  BitfinexTicker getTicker(@PathParam("symbol") String symbol)
      throws IOException, BitfinexExceptionV1;

  @GET
  @Path("book/{symbol}")
  BitfinexDepth getBook(
      @PathParam("symbol") String symbol,
      @QueryParam("limit_bids") int limit_bids,
      @QueryParam("limit_asks") int limit_asks)
      throws IOException, BitfinexExceptionV1;

  @GET
  @Path("book/{symbol}")
  BitfinexDepth getBook(@PathParam("symbol") String symbol) throws IOException, BitfinexExceptionV1;

  @GET
  @Path("lendbook/{currency}")
  BitfinexLendDepth getLendBook(
      @PathParam("currency") String currency,
      @QueryParam("limit_bids") int limit_bids,
      @QueryParam("limit_asks") int limit_asks)
      throws IOException, BitfinexExceptionV1;

  @GET
  @Path("trades/{symbol}")
  BitfinexTrade[] getTrades(
      @PathParam("symbol") String symbol, @QueryParam("timestamp") long timestamp)
      throws IOException, BitfinexExceptionV1;

  @GET
  @Path("lends/{currency}")
  BitfinexLend[] getLends(
      @PathParam("currency") String currency,
      @QueryParam("timestamp") long timestamp,
      @QueryParam("limit_trades") int limit_trades)
      throws IOException, BitfinexExceptionV1;

  @GET
  @Path("symbols")
  Set<String> getSymbols() throws IOException, BitfinexExceptionV1;

  @GET
  @Path("symbols_details")
  List<BitfinexSymbolDetail> getSymbolsDetails() throws IOException, BitfinexExceptionV1;
}
