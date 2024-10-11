package org.knowm.xchange.vaultoro;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.vaultoro.dto.account.VaultoroBalancesResponse;
import org.knowm.xchange.vaultoro.dto.trade.VaultoroCancelOrderResponse;
import org.knowm.xchange.vaultoro.dto.trade.VaultoroNewOrderResponse;
import org.knowm.xchange.vaultoro.dto.trade.VaultoroOrdersResponse;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.math.BigDecimal;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface VaultoroAuthenticated extends Vaultoro {

  @GET
  @Path("1/balance")
  VaultoroBalancesResponse getBalances(
      @QueryParam("nonce") SynchronizedValueFactory<Long> nonce,
      @QueryParam("apikey") String apiKey,
      @HeaderParam("X-Signature") ParamsDigest signature)
      throws IOException, VaultoroException;

  @GET
  @Path("1/orders")
  VaultoroOrdersResponse getOrders(
      @QueryParam("nonce") SynchronizedValueFactory<Long> nonce,
      @QueryParam("apikey") String apiKey,
      @HeaderParam("X-Signature") ParamsDigest signature)
      throws IOException, VaultoroException;

  @POST
  @Path("1/buy/{symbol}/{type}")
  VaultoroNewOrderResponse buy(
      @PathParam("symbol") String symbol,
      @PathParam("type") String type,
      @QueryParam("nonce") SynchronizedValueFactory<Long> nonce,
      @QueryParam("apikey") String apiKey,
      @QueryParam("btc") BigDecimal btc,
      @QueryParam("price") BigDecimal price,
      @HeaderParam("X-Signature") ParamsDigest signature)
      throws IOException, VaultoroException;

  @POST
  @Path("1/sell/{symbol}/{type}")
  VaultoroNewOrderResponse sell(
      @PathParam("symbol") String symbol,
      @PathParam("type") String type,
      @QueryParam("nonce") SynchronizedValueFactory<Long> nonce,
      @QueryParam("apikey") String apiKey,
      @QueryParam("gld") BigDecimal gld,
      @QueryParam("price") BigDecimal price,
      @HeaderParam("X-Signature") ParamsDigest signature)
      throws IOException, VaultoroException;

  @POST
  @Path("1/cancel/{orderid}")
  VaultoroCancelOrderResponse cancel(
      @PathParam("orderid") String orderid,
      @QueryParam("nonce") SynchronizedValueFactory<Long> nonce,
      @QueryParam("apikey") String apiKey,
      @HeaderParam("X-Signature") ParamsDigest signature)
      throws IOException, VaultoroException;
}
