package org.knowm.xchange.coindirect;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.coindirect.dto.CoindirectException;
import org.knowm.xchange.coindirect.dto.account.CoindirectAccountChannel;
import org.knowm.xchange.coindirect.dto.account.CoindirectWallet;
import org.knowm.xchange.coindirect.dto.trade.CoindirectOrder;
import org.knowm.xchange.coindirect.dto.trade.CoindirectOrderRequest;
import si.mazi.rescu.ParamsDigest;

import java.io.IOException;
import java.util.List;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface CoindirectAuthenticated extends Coindirect {
  public static final String AUTHORIZATION = "Authorization";

  @GET
  @Path("api/wallet")
  List<CoindirectWallet> listWallets(
      @QueryParam("max") long max, @HeaderParam("Authorization") ParamsDigest signer)
      throws IOException, CoindirectException;

  @GET
  @Path("api/v1/exchange/order")
  List<CoindirectOrder> listExchangeOrders(
      @QueryParam("symbol") String symbol,
      @QueryParam("completed") boolean completed,
      @QueryParam("offset") long offset,
      @QueryParam("max") long max,
      @HeaderParam("Authorization") ParamsDigest signer)
      throws IOException, CoindirectException;

  @POST
  @Path("api/v1/exchange/order")
  @Consumes(MediaType.APPLICATION_JSON)
  CoindirectOrder placeExchangeOrder(
      CoindirectOrderRequest coindirectOrderRequest,
      @HeaderParam("Authorization") ParamsDigest signer);

  @DELETE
  @Path("api/v1/exchange/order/{uuid}")
  CoindirectOrder cancelExchangeOrder(
      @PathParam("uuid") String uuid, @HeaderParam("Authorization") ParamsDigest signer);

  @GET
  @Path("api/v1/exchange/order/read/{uuid}")
  CoindirectOrder getExchangeOrder(
      @PathParam("uuid") String uuid, @HeaderParam("Authorization") ParamsDigest signer);

  @GET
  @Path("api/account/channel")
  CoindirectAccountChannel getAccountChannel(@HeaderParam("Authorization") ParamsDigest signer)
      throws IOException, CoindirectException;
}
