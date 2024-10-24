package org.knowm.xchange.bitbay.v3;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.bitbay.v3.dto.BitbayBalanceHistoryResponse;
import org.knowm.xchange.bitbay.v3.dto.BitbayBalances;
import org.knowm.xchange.bitbay.v3.dto.trade.BitbayUserTrades;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.util.UUID;

/** @author walec51 */
@SuppressWarnings("rawtypes")
@Path("/rest/")
@Produces(MediaType.APPLICATION_JSON)
public interface BitbayAuthenticated {

  @GET
  @Path("trading/history/transactions")
  BitbayUserTrades getTransactionHistory(
      @HeaderParam("API-Key") String apiKey,
      @HeaderParam("API-Hash") ParamsDigest sign,
      @HeaderParam("Request-Timestamp") SynchronizedValueFactory<Long> timestamp,
      @HeaderParam("Operation-Id") UUID operationId,
      @QueryParam("query") String query)
      throws IOException;

  @GET
  @Path("balances/BITBAY/balance")
  BitbayBalances balance(
      @HeaderParam("API-Key") String apiKey,
      @HeaderParam("API-Hash") ParamsDigest sign,
      @HeaderParam("Request-Timestamp") SynchronizedValueFactory<Long> timestamp,
      @HeaderParam("Operation-Id") UUID operationId)
      throws IOException;

  @GET
  @Path("balances/BITBAY/history")
  BitbayBalanceHistoryResponse balanceHistory(
      @HeaderParam("API-Key") String apiKey,
      @HeaderParam("API-Hash") ParamsDigest sign,
      @HeaderParam("Request-Timestamp") SynchronizedValueFactory<Long> timestamp,
      @HeaderParam("Operation-Id") UUID operationId,
      @QueryParam("query") String query)
      throws IOException;
}
