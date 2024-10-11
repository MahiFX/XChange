package org.knowm.xchange.cryptowatch;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.cryptowatch.dto.marketdata.results.*;

/** @author massi.gerardi */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface Cryptowatch {

  @GET
  @Path("markets/{market}/{pair}/ohlc")
  CryptowatchOHLCResult getOHLC(
      @PathParam("market") String market,
      @PathParam("pair") String pair,
      @QueryParam("before") Long before,
      @QueryParam("after") Long after,
      @QueryParam("periods") Integer periods);

  @GET
  @Path("markets/{market}/{pair}/price")
  CryptowatchPriceResult getPrice(
      @PathParam("market") String market, @PathParam("pair") String pair);

  @GET
  @Path("markets/{market}/{pair}/summary")
  CryptowatchSummaryResult getTicker(
      @PathParam("market") String market, @PathParam("pair") String pair);

  @GET
  @Path("markets/{market}/{pair}/trades")
  CryptowatchTradesResult getTrades(
      @PathParam("market") String market,
      @PathParam("pair") String pair,
      @QueryParam("limit") Integer limit,
      @QueryParam("since") Long since);

  @GET
  @Path("markets/{market}/{pair}/orderbook")
  CryptowatchOrderBookResult getOrderBook(
      @PathParam("market") String market, @PathParam("pair") String pair);

  @GET
  @Path("assets")
  CryptowatchAssetsResult getAssets();

  @GET
  @Path("pairs")
  CryptowatchAssetPairsResult getAssetPairs();
}
