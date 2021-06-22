package info.bitrich.xchangestream.binance;

import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.trade.BinanceListenKey;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceStreamingCommon extends BinanceAuthenticated {
    String X_MBX_APIKEY = "X-MBX-APIKEY";

    /**
     * Returns a listen key for websocket login.
     *
     * @param apiKey the api key
     * @return
     * @throws BinanceException
     * @throws IOException
     */
    @POST
    @Path("/api/v3/userDataStream")
    BinanceListenKey startUserDataStream(@HeaderParam(X_MBX_APIKEY) String apiKey)
            throws IOException, BinanceException;

    /**
     * Keeps the authenticated websocket session alive.
     *
     * @param apiKey    the api key
     * @param listenKey the api secret
     * @return
     * @throws BinanceException
     * @throws IOException
     */
    @PUT
    @Path("/api/v3/userDataStream?listenKey={listenKey}")
    Map<?, ?> keepAliveUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;

    /**
     * Closes the websocket authenticated connection.
     *
     * @param apiKey    the api key
     * @param listenKey the api secret
     * @return
     * @throws BinanceException
     * @throws IOException
     */
    @DELETE
    @Path("/api/v3/userDataStream?listenKey={listenKey}")
    Map<?, ?> closeUserDataStream(
            @HeaderParam(X_MBX_APIKEY) String apiKey, @PathParam("listenKey") String listenKey)
            throws IOException, BinanceException;
}
