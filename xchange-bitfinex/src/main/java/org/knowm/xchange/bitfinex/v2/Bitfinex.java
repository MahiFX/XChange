package org.knowm.xchange.bitfinex.v2;

import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.bitfinex.v2.dto.BitfinexExceptionV2;
import org.knowm.xchange.bitfinex.v2.dto.marketdata.*;

import java.io.IOException;
import java.util.List;

@Path("v2")
@Produces(MediaType.APPLICATION_JSON)
public interface Bitfinex {

  @GET
  @Path("platform/status")
  Integer[] getPlatformStatus() throws IOException, BitfinexExceptionV2;

  @GET
  @Path("tickers")
  List<ArrayNode> getTickers(@QueryParam("symbols") String symbols)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("status/{type}")
  List<Status> getStatus(@PathParam("type") String type, @QueryParam("keys") String symbols)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("/trades/{symbol}/hist")
  BitfinexPublicFundingTrade[] getPublicFundingTrades(
      @PathParam("symbol") String fundingSymbol,
      @QueryParam("limit") int limit,
      @QueryParam("start") long startTimestamp,
      @QueryParam("end") long endTimestamp,
      @QueryParam("sort") int sort)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("/trades/{symbol}/hist")
  BitfinexPublicTrade[] getPublicTrades(
      @PathParam("symbol") String fundingSymbol,
      @QueryParam("limit") int limit,
      @QueryParam("start") long startTimestamp,
      @QueryParam("end") long endTimestamp,
      @QueryParam("sort") int sort)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("candles/trade:{candlePeriod}:{symbol}:{fundingPeriod}/hist")
  List<BitfinexCandle> getHistoricFundingCandles(
      @PathParam("candlePeriod") String candlePeriod,
      @PathParam("symbol") String currency,
      @PathParam("fundingPeriod") String fundingPeriod,
      @QueryParam("limit") int limit)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("/candles/trade:{candlePeriod}:{symbol}/hist")
  List<BitfinexCandle> getHistoricCandles(
      @PathParam("candlePeriod") String candlePeriod,
      @PathParam("symbol") String currency,
      @QueryParam("limit") Integer limit,
      @QueryParam("start") Long startTimestamp,
      @QueryParam("end") Long endTimestamp,
      @QueryParam("sort") Integer sort)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("stats1/{key}:{size}:{symbol}:{side}/hist")
  List<BitfinexStats> getStats(
      @PathParam("key") String key,
      @PathParam("size") String size,
      @PathParam("symbol") String symbol,
      @PathParam("side") String side,
      @QueryParam("sort") Integer sort,
      @QueryParam("start") Long startTimestamp,
      @QueryParam("end") Long endTimestamp,
      @QueryParam("limit") Integer limit)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("book/{symbol}/{precision}")
  List<BitfinexTradingOrder> tradingBook(
      @PathParam("symbol") String symbol,
      @PathParam("precision") BookPrecision precision,
      @QueryParam("len") Integer len)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("book/{symbol}/R0")
  List<BitfinexTradingRawOrder> tradingBookRaw(
      @PathParam("symbol") String symbol, @QueryParam("len") Integer len)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("book/{symbol}/{precision}")
  List<BitfinexFundingOrder> fundingBook(
      @PathParam("symbol") String symbol,
      @PathParam("precision") BookPrecision precision,
      @QueryParam("len") Integer len)
      throws IOException, BitfinexExceptionV2;

  @GET
  @Path("book/{symbol}/R0")
  List<BitfinexFundingRawOrder> fundingBookRaw(
      @PathParam("symbol") String symbol, @QueryParam("len") Integer len)
      throws IOException, BitfinexExceptionV2;
}
