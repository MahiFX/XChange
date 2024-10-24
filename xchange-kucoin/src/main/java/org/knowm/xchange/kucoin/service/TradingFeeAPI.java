package org.knowm.xchange.kucoin.service;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.kucoin.dto.response.KucoinResponse;
import org.knowm.xchange.kucoin.dto.response.TradeFeeResponse;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.util.List;

@Path("api/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface TradingFeeAPI {

  /**
   * Get basic fee rate of users.
   *
   * @return basic trading fee information
   */
  @GET
  @Path("/base-fee")
  KucoinResponse<TradeFeeResponse> getBaseFee(
      @HeaderParam(APIConstants.API_HEADER_KEY) String apiKey,
      @HeaderParam(APIConstants.API_HEADER_SIGN) ParamsDigest signature,
      @HeaderParam(APIConstants.API_HEADER_TIMESTAMP) SynchronizedValueFactory<Long> nonce,
      @HeaderParam(APIConstants.API_HEADER_PASSPHRASE) String apiPassphrase)
      throws IOException;

  @GET
  @Path("/trade-fees")
  KucoinResponse<List<TradeFeeResponse>> getTradeFee(
      @HeaderParam(APIConstants.API_HEADER_KEY) String apiKey,
      @HeaderParam(APIConstants.API_HEADER_SIGN) ParamsDigest signature,
      @HeaderParam(APIConstants.API_HEADER_TIMESTAMP) SynchronizedValueFactory<Long> nonce,
      @HeaderParam(APIConstants.API_HEADER_PASSPHRASE) String apiPassphrase,
      @QueryParam("symbols") String symbols)
      throws IOException;
}
