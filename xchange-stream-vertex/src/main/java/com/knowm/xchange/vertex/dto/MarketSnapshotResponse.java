package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.Map;

@ToString
@Getter
public class MarketSnapshotResponse {


  private MarketSnapshot[] snapshots;

  @ToString
  @Getter
  public static class MarketSnapshot {

    private long timestamp;
    private long cumulative_users;
    private long daily_active_users;
    private Map<Long, BigInteger> cumulative_trades;
    private Map<Long, BigInteger> cumulative_volumes;
    private Map<Long, BigInteger> cumulative_trade_sizes;
    private Map<Long, BigInteger> cumulative_taker_fees;
    private Map<Long, BigInteger> cumulative_sequencer_fees;
    private Map<Long, BigInteger> cumulative_maker_fees;
    private Map<Long, BigInteger> cumulative_liquidation_amounts;
    private Map<Long, BigInteger> open_interests;
    private Map<Long, BigInteger> total_deposits;
    private Map<Long, BigInteger> total_borrows;
    private Map<Long, BigInteger> funding_rates;
    private Map<Long, BigInteger> deposit_rates;
    private Map<Long, BigInteger> borrow_rates;
    private Map<Long, BigInteger> cumulative_inflows;
    private Map<Long, BigInteger> cumulative_outflows;
    private BigInteger tvl;


  }
}
