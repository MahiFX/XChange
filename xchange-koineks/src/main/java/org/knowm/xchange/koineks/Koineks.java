package org.knowm.xchange.koineks;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.koineks.dto.marketdata.KoineksTicker;

import java.io.IOException;

/** Created by semihunaldi on 05/12/2017 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface Koineks {
  @GET
  @Path("ticker")
  KoineksTicker getTicker() throws IOException;
}
