package com.knowm.xchange.vertex.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowm.xchange.vertex.dto.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;


@Produces(MediaType.APPLICATION_JSON)
@Path("")
public interface VertexArchiveApi {


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  RewardsList rewards(RewardsRequest req);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Map<String, ProductSnapshotResponse> productSnapshots(ProductSnapshotRequest req);


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  MarketSnapshotResponse marketSnapshots(MarketSnapshotRequest req);


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  Map<String, FundingRateResponse> fundingRates(FundingRateRequest req);


  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  JsonNode indexerRequest(JsonNode req);


}


