package org.knowm.xchange.binance;

import org.knowm.xchange.binance.dto.*;
import org.knowm.xchange.binance.dto.meta.BinanceTime;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.dto.trade.BinanceCancelledOrder;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("RestParamTypeInspection")
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceFuturesAuthenticated extends BinanceFutures {

    String SIGNATURE = "signature";
    String X_MBX_APIKEY = "X-MBX-APIKEY";

    @Override
    @GET
    @Path("v1/time")
    BinanceTime time() throws IOException;

    /**
     * Current exchange trading rules and symbol information.
     *
     * @return
     * @throws IOException
     */
    @Override
    @GET
    @Path("v1/exchangeInfo")
    BinanceExchangeInfo exchangeInfo() throws IOException;

    @POST
    @Path("v1/order")
    BinanceFuturesOrder newOrder(
            @FormParam("symbol") String symbol,
            @FormParam("side") OrderSide side,
            @FormParam("positionSide") PositionSide positionSide,
            @FormParam("type") OrderType type,
            @FormParam("timeInForce") TimeInForce timeInForce,
            @FormParam("quantity") BigDecimal quantity,
            @FormParam("reduceOnly") Boolean reduceOnly,
            @FormParam("price") BigDecimal price,
            @FormParam("newClientOrderId") String newClientOrderId,
            @FormParam("stopPrice") BigDecimal stopPrice,
            @FormParam("closePosition") Boolean closePosition,
            @FormParam("activationPrice") BigDecimal activationPrice,
            @FormParam("callbackRate") BigDecimal callbackRate,
            @FormParam("workingType") WorkingType workingType,
            @FormParam("priceProtect") Boolean priceProtect,
            @FormParam("recvWindow") Long recvWindow,
            @FormParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @GET
    @Path("v1/order")
    BinanceFuturesOrder getOrder(
            @QueryParam("symbol") String symbol,
            @QueryParam("orderId") Long orderId,
            @QueryParam("origClientOrderId") String origClientOrderId,
            @QueryParam("recvWindow") Long recvWindow,
            @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @DELETE
    @Path("v1/order")
    BinanceFuturesOrder cancelOrder(
            @QueryParam("symbol") String symbol,
            @QueryParam("orderId") Long orderId,
            @QueryParam("origClientOrderId") String origClientOrderId,
            @QueryParam("recvWindow") Long recvWindow,
            @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @DELETE
    @Path("v1/allOpenOrders")
    List<BinanceCancelledOrder> cancelAllOpenOrders(
            @QueryParam("symbol") String symbol,
            @QueryParam("recvWindow") Long recvWindow,
            @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @GET
    @Path("v1/openOrder")
    BinanceFuturesOrder getOpenOrder(
            @QueryParam("symbol") String symbol,
            @QueryParam("orderId") Long orderId,
            @QueryParam("origClientOrderId") String origClientOrderId,
            @QueryParam("recvWindow") Long recvWindow,
            @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @GET
    @Path("v1/openOrders")
    List<BinanceFuturesOrder> getAllOpenOrders(
            @QueryParam("symbol") String symbol,
            @QueryParam("recvWindow") Long recvWindow,
            @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;

    @GET
    @Path("v1/positionRisk")
    List<BinancePosition> getOpenPositions(
            @QueryParam("symbol") String symbol,
            @QueryParam("recvWindow") Long recvWindow,
            @Nonnull @QueryParam("timestamp") SynchronizedValueFactory<Long> timestamp,
            @HeaderParam(X_MBX_APIKEY) String apiKey,
            @QueryParam(SIGNATURE) ParamsDigest signature)
            throws IOException, BinanceException;
}
