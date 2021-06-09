package org.knowm.xchange.binance;

import org.knowm.xchange.binance.dto.*;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigDecimal;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceFuturesAuthenticated extends BinanceAuthenticated {

    /**
     * Current exchange trading rules and symbol information.
     *
     * @return
     * @throws IOException
     */
    @Override
    @GET
    @Path("fapi/v1/exchangeInfo")
    BinanceExchangeInfo exchangeInfo() throws IOException;

    /**
     * @param symbol
     * @param limit  optional, default 100 max 5000. Valid limits: [5, 10, 20, 50, 100, 500, 1000,
     *               5000]
     * @return
     * @throws IOException
     * @throws BinanceException
     */
    @Override
    @GET
    @Path("fapi/v1/depth")
    BinanceOrderbook depth(@QueryParam("symbol") String symbol, @QueryParam("limit") Integer limit) throws IOException, BinanceException;

    @POST
    @Path("fapi/v1/order")
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
}
