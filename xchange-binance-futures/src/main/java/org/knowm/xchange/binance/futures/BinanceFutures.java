package org.knowm.xchange.binance.futures;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.binance.BinanceAuthenticated;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;

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
