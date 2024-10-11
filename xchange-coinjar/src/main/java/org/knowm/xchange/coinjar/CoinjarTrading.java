package org.knowm.xchange.coinjar;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.coinjar.dto.CoinjarOrder;
import org.knowm.xchange.coinjar.dto.trading.CoinjarAccount;
import org.knowm.xchange.coinjar.dto.trading.CoinjarFills;
import org.knowm.xchange.coinjar.dto.trading.CoinjarOrderRequest;
import org.knowm.xchange.coinjar.dto.trading.CoinjarProduct;

import java.io.IOException;
import java.util.List;

@Produces({"application/json"})
@Path("/")
public interface CoinjarTrading {

  @GET
  @Path("/products")
  List<CoinjarProduct> getProducts() throws CoinjarException, IOException;

  @GET
  @Path("/accounts")
  List<CoinjarAccount> getAccounts(@HeaderParam("Authorization") String authHeader)
      throws CoinjarException, IOException;

  @POST
  @Path("/orders")
  @Consumes(MediaType.APPLICATION_JSON)
  CoinjarOrder placeOrder(
      @HeaderParam("Authorization") String authHeader, CoinjarOrderRequest request)
      throws CoinjarException, IOException;

  @GET
  @Path("/orders")
  List<CoinjarOrder> getOpenOrders(
      @HeaderParam("Authorization") String authHeader, @QueryParam("cursor") Integer cursor)
      throws CoinjarException, IOException;

  @GET
  @Path("/orders/all")
  List<CoinjarOrder> getAllOrders(
      @HeaderParam("Authorization") String authHeader, @QueryParam("cursor") Integer cursor)
      throws CoinjarException, IOException;

  @GET
  @Path("/orders/{id}")
  CoinjarOrder getOrder(@HeaderParam("Authorization") String authHeader, @PathParam("id") String id)
      throws CoinjarException, IOException;

  @GET
  @Path("/fills")
  CoinjarFills getFills(
      @HeaderParam("Authorization") String authHeader,
      @QueryParam("cursor") String cursor,
      @QueryParam("product_id") String productId,
      @QueryParam("oid") String oid)
      throws CoinjarException, IOException;

  @DELETE
  @Path("/orders/{id}")
  CoinjarOrder cancelOrder(
      @HeaderParam("Authorization") String authHeader, @PathParam("id") String id)
      throws CoinjarException, IOException;
}
