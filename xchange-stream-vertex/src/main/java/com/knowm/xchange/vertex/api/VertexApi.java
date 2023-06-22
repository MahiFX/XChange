package com.knowm.xchange.vertex.api;

import com.knowm.xchange.vertex.dto.RewardsList;
import com.knowm.xchange.vertex.dto.RewardsRequest;
import com.knowm.xchange.vertex.dto.Symbol;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
public interface VertexApi {


    @POST
    @Path("/indexer")
    RewardsList rewards(RewardsRequest req);


    @GET
    @Path("/symbols")
    Symbol[] symbols();
}


