package org.knowm.xchange.gemini.v1;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.gemini.v1.dto.GeminiException;
import org.knowm.xchange.gemini.v1.dto.account.*;
import org.knowm.xchange.gemini.v1.dto.trade.*;
import si.mazi.rescu.ParamsDigest;

import java.io.IOException;

@Path("v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeminiAuthenticated extends Gemini {

  @POST
  @Path("order/new")
  GeminiOrderStatusResponse newOrder(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiNewOrderRequest newOrderRequest)
      throws IOException, GeminiException;

  @POST
  @Path("balances")
  GeminiBalancesResponse[] balances(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiBalancesRequest balancesRequest)
      throws IOException, GeminiException;

  @POST
  @Path("order/cancel")
  GeminiOrderStatusResponse cancelOrders(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiCancelOrderRequest cancelOrderRequest)
      throws IOException, GeminiException;

  @POST
  @Path("order/cancel/session")
  GeminiCancelAllOrdersResponse cancelAllSessionOrders(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiCancelAllOrdersRequest cancelAllOrdersRequest)
      throws IOException, GeminiException;

  @POST
  @Path("order/cancel/all")
  GeminiCancelAllOrdersResponse cancelAllOrders(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiCancelAllOrdersRequest cancelAllOrdersRequest)
      throws IOException, GeminiException;

  @POST
  @Path("orders")
  GeminiOrderStatusResponse[] activeOrders(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiNonceOnlyRequest nonceOnlyRequest)
      throws IOException, GeminiException;

  @POST
  @Path("order/status")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  GeminiOrderStatusResponse orderStatus(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiOrderStatusRequest orderStatusRequest)
      throws IOException, GeminiException;

  @POST
  @Path("mytrades")
  GeminiTradeResponse[] pastTrades(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiPastTradesRequest pastTradesRequest)
      throws IOException, GeminiException;

  @POST
  @Path("notionalvolume")
  GeminiTrailingVolumeResponse TrailingVolume(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      GeminiTrailingVolumeRequest pastTradesRequest)
      throws IOException, GeminiException;

  @POST
  @Path("withdraw/{currency}")
  GeminiWithdrawalResponse withdraw(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      @PathParam("currency") String currency,
      GeminiWithdrawalRequest withdrawalRequest)
      throws IOException, GeminiException;

  @POST
  @Path("deposit/{currency}/newAddress")
  GeminiDepositAddressResponse requestNewAddress(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signature,
      @PathParam("currency") String currency,
      GeminiDepositAddressRequest depositRequest)
      throws IOException, GeminiException;

  @POST
  @Path("transfers")
  GeminiTransfersResponse transfers(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payloadCreator,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signatureCreator,
      GeminiTransfersRequest request)
      throws IOException, GeminiException;

  @POST
  @Path("heartbeat")
  GeminiOrderStatusResponse heartBeat(
      @HeaderParam("X-GEMINI-APIKEY") String apiKey,
      @HeaderParam("X-GEMINI-PAYLOAD") ParamsDigest payloadCreator,
      @HeaderParam("X-GEMINI-SIGNATURE") ParamsDigest signatureCreator,
      GeminiNonceOnlyRequest nonceOnlyRequest)
      throws IOException, GeminiException;
}
