package com.knowm.xchange.vertex.dto;

import lombok.ToString;

@ToString
public class MarketSnapshotRequest {

  private final MarketSnapshotParams market_snapshots;


  public MarketSnapshotRequest(MarketSnapshotParams marketSnapshots) {
    market_snapshots = marketSnapshots;
  }

  @ToString
  public static class MarketSnapshotParams {
    private final Interval interval;
    private final long[] product_ids;

    @ToString
    public static class Interval {
      private final int count;
      private final int granularity;
      private final long max_time;

      public Interval(int count, int granularity, long max_time) {
        this.count = count;
        this.granularity = granularity;
        this.max_time = max_time;
      }
    }

    public MarketSnapshotParams(Interval interval, long[] product_ids) {
      this.interval = interval;
      this.product_ids = product_ids;
    }
  }
}
