package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ProductSnapshotRequest {

  private final ProductSnapshotParams product_snapshots;

  public ProductSnapshotRequest(ProductSnapshotParams product_snapshots) {
    this.product_snapshots = product_snapshots;
  }

  @ToString
  @Getter
  public static class ProductSnapshotParams {

    private final String[] product_id;


    private final Long max_time;

    public ProductSnapshotParams(String[] product_id, Long max_time) {
      this.product_id = product_id;
      this.max_time = max_time;
    }
  }
}
