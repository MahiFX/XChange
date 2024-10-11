package org.knowm.xchange.bitfinex.v1;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.bitfinex.v1.dto.BitfinexExceptionV1;
import org.knowm.xchange.bitfinex.v1.dto.account.*;
import org.knowm.xchange.bitfinex.v1.dto.trade.*;
import si.mazi.rescu.ParamsDigest;

import java.io.IOException;

@Path("v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BitfinexAuthenticated extends Bitfinex {

  @POST
  @Path("account_infos")
  BitfinexAccountInfosResponse[] accountInfos(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNonceOnlyRequest accountInfosRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("account_fees")
  BitfinexAccountFeesResponse accountFees(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNonceOnlyRequest accountInfosRequest)
      throws IOException;

  @POST
  @Path("order/new")
  BitfinexOrderStatusResponse newOrder(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNewOrderRequest newOrderRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/new/multi")
  BitfinexNewOrderMultiResponse newOrderMulti(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNewOrderMultiRequest newOrderMultiRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("offer/new")
  BitfinexOfferStatusResponse newOffer(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNewOfferRequest newOfferRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("balances")
  BitfinexBalancesResponse[] balances(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexBalancesRequest balancesRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("account_infos")
  BitfinexTradingFeeResponse[] tradingFees(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexTradingFeesRequest tradingFeeRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/cancel")
  BitfinexOrderStatusResponse cancelOrders(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexCancelOrderRequest cancelOrderRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/cancel/all")
  BitfinexOrderStatusResponse cancelAllOrders(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexCancelAllOrdersRequest cancelAllOrdersRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/cancel/multi")
  BitfinexCancelOrderMultiResponse cancelOrderMulti(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexCancelOrderMultiRequest cancelOrderRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/cancel/replace")
  BitfinexOrderStatusResponse replaceOrder(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexReplaceOrderRequest newOrderRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("offer/cancel")
  BitfinexOfferStatusResponse cancelOffer(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexCancelOfferRequest cancelOfferRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("orders")
  BitfinexOrderStatusResponse[] activeOrders(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNonceOnlyRequest nonceOnlyRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("orders/hist")
  BitfinexOrderStatusResponse[] ordersHist(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexOrdersHistoryRequest ordersHistoryRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("offers")
  BitfinexOfferStatusResponse[] activeOffers(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNonceOnlyRequest nonceOnlyRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("positions")
  BitfinexActivePositionsResponse[] activePositions(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexNonceOnlyRequest nonceOnlyRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("order/status")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BitfinexOrderStatusResponse orderStatus(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexOrderStatusRequest orderStatusRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("offer/status")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  BitfinexOfferStatusResponse offerStatus(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexOfferStatusRequest offerStatusRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("mytrades")
  BitfinexTradeResponse[] pastTrades(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexPastTradesRequest pastTradesRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("mytrades_funding")
  BitfinexFundingTradeResponse[] pastFundingTrades(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexPastFundingTradesRequest bitfinexPastFundingTradesRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("credits")
  BitfinexCreditResponse[] activeCredits(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexActiveCreditsRequest activeCreditsRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("margin_infos")
  BitfinexMarginInfosResponse[] marginInfos(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexMarginInfosRequest marginInfosRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("withdraw")
  BitfinexWithdrawalResponse[] withdraw(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexWithdrawalRequest withdrawalRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("deposit/new")
  BitfinexDepositAddressResponse requestDeposit(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexDepositAddressRequest depositRequest)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("history/movements")
  BitfinexDepositWithdrawalHistoryResponse[] depositWithdrawalHistory(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexDepositWithdrawalHistoryRequest request)
      throws IOException, BitfinexExceptionV1;

  @POST
  @Path("history")
  BitfinexBalanceHistoryResponse[] balanceHistory(
      @HeaderParam("X-BFX-APIKEY") String apiKey,
      @HeaderParam("X-BFX-PAYLOAD") ParamsDigest payload,
      @HeaderParam("X-BFX-SIGNATURE") ParamsDigest signature,
      BitfinexBalanceHistoryRequest balanceHistoryRequest)
      throws IOException, BitfinexExceptionV1;
}
