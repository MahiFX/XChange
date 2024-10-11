package com.knowm.xchange.vertex.api;

import com.knowm.xchange.vertex.dto.SymbolListingResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Produces(MediaType.APPLICATION_JSON)
@Path("")
public interface VertexQueryApi {

  @GET
  @Path("/query?type=symbols")
  SymbolListingResponse symbols();
}


