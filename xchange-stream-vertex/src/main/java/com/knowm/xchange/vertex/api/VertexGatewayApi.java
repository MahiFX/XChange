package com.knowm.xchange.vertex.api;

import com.knowm.xchange.vertex.dto.SymbolListingResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface VertexGatewayApi {

  @GET
  @Path("/query?type=symbols")
  SymbolListingResponse symbols();
}


