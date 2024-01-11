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

    private final Long[] product_ids;


    private final Long max_time;

    public ProductSnapshotParams(Long[] product_id, Long max_time) {
      this.product_ids = product_id;
      this.max_time = max_time;
    }
  }
}
