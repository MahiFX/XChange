package info.bitrich.xchangestream.binance;

import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.trade.BinanceListenKey;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceFuturesStreaming extends BinanceStreamingCommon, BinanceFuturesAuthenticated {
    String X_MBX_APIKEY = "X-MBX-APIKEY";

    @Override
    @POST
    @Path("/fapi/v1/listenKey")
    BinanceListenKey startUserDataStream(@HeaderParam(X_MBX_APIKEY) String apiKey)
            throws IOException, BinanceException;

    @Override
    @PUT
    @Path("/fapi/v1/listenKey?listenKey={listenKey}")
    Map<?, ?> keepAliveUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;

    @Override
    @DELETE
    @Path("/fapi/v1/listenKey?listenKey={listenKey}")
    Map<?, ?> closeUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;
}
