package info.bitrich.xchangestream.binance;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.trade.BinanceListenKey;

import java.io.IOException;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceStreaming extends BinanceStreamingCommon, BinanceAuthenticated {
    String X_MBX_APIKEY = "X-MBX-APIKEY";

    @Override
    @POST
    @Path("/api/v3/userDataStream")
    BinanceListenKey startUserDataStream(@HeaderParam(X_MBX_APIKEY) String apiKey)
            throws IOException, BinanceException;

    @Override
    @PUT
    @Path("/api/v3/userDataStream?listenKey={listenKey}")
    Map<?, ?> keepAliveUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;

    @Override
    @DELETE
    @Path("/api/v3/userDataStream?listenKey={listenKey}")
    Map<?, ?> closeUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;
}
