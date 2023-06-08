package org.knowm.xchange.binance.futures;

import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface BinanceFutures extends BinanceAuthenticated {

    @Override
    @GET
    @Path("v1/depth")
    BinanceOrderbook depth(
            @QueryParam("symbol") String symbol,
            @QueryParam("limit") Integer limit)
            throws IOException, BinanceException;
}
