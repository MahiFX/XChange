package info.bitrich.xchangestream.binance;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.trade.BinanceListenKey;
import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;

import java.io.IOException;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceFuturesStreaming extends BinanceStreamingCommon, BinanceFuturesAuthenticated {
    String X_MBX_APIKEY = "X-MBX-APIKEY";

    @Override
    @POST
    @Path("/v1/listenKey")
    BinanceListenKey startUserDataStream(@HeaderParam(X_MBX_APIKEY) String apiKey)
            throws IOException, BinanceException;

    @Override
    @PUT
    @Path("/v1/listenKey?listenKey={listenKey}")
    Map<?, ?> keepAliveUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;

    @Override
    @DELETE
    @Path("/v1/listenKey?listenKey={listenKey}")
    Map<?, ?> closeUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;
}
