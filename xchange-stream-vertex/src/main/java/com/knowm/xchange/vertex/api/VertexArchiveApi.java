package com.knowm.xchange.vertex.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowm.xchange.vertex.dto.*;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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


