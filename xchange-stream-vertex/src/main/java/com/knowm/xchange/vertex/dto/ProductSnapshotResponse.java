package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class ProductSnapshotResponse {
/*
{
    "product_id": 1,
    "submission_idx": "459743",
    "product": {
      "spot": {
        "product_id": 1,
        "oracle_price_x18": "31582395488073320408472",
        "risk": {
          "long_weight_initial_x18": "900000000000000000",
          "short_weight_initial_x18": "1100000000000000000",
          "long_weight_maintenance_x18": "950000000000000000",
          "short_weight_maintenance_x18": "1050000000000000000",
          "large_position_penalty_x18": "0"
        },
        "config": {
          "token": "0x5cc7c91690b2cbaee19a513473d73403e13fb431",
          "interest_inflection_util_x18": "800000000000000000",
          "interest_floor_x18": "10000000000000000",
          "interest_small_cap_x18": "40000000000000000",
          "interest_large_cap_x18": "1000000000000000000"
        },
        "state": {
          "cumulative_deposits_multiplier_x18": "1005266691979081067",
          "cumulative_borrows_multiplier_x18": "1008464332498400342",
          "total_deposits_normalized": "3853380366798127292498",
          "total_borrows_normalized": "3171903194179038855701"
        },
        "lp_state": {
          "supply": "485670917658672273450851",
          "quote": {
            "amount": "533498653674283527525638",
            "last_cumulative_multiplier_x18": "1000000000047911804"
          },
          "base": {
            "amount": "16862535452736644443",
            "last_cumulative_multiplier_x18": "1005266691979081067"
          }
        },
        "book_info": {
          "size_increment": "1000000000000000",
          "price_increment_x18": "1000000000000000000",
          "min_size": "10000000000000000",
          "collected_fees": "460839041331226808022323",
          "lp_spread_x18": "3000000000000000"
        }
      }
    }
  }
 */

  private String product_id;
  private String submission_idx;
  private Product product;

  @ToString
  @Getter
  public static class Product {
    private Spot spot;
    private Perp perp;

  }

  @ToString
  @Getter
  public static class Spot {
    private String product_id;
    private String oracle_price_x18;
    private Risk risk;
    private Config config;
    private State state;
    private LpState lp_state;
    private BookInfo book_info;

  }

  @ToString
  @Getter
  public static class Risk {
    private String long_weight_initial_x18;
    private String short_weight_initial_x18;
    private String long_weight_maintenance_x18;
    private String short_weight_maintenance_x18;
    private String large_position_penalty_x18;

  }

  @ToString
  @Getter
  public static class Config {
    private String token;
    private String interest_inflection_util_x18;
    private String interest_floor_x18;
    private String interest_small_cap_x18;
    private String interest_large_cap_x18;
  }

  @ToString
  @Getter
  public static class State {
    private String cumulative_deposits_multiplier_x18;
    private String cumulative_borrows_multiplier_x18;
    private String total_deposits_normalized;
    private String total_borrows_normalized;
  }

  @ToString
  @Getter
  public static class LpState {
    private String supply;
    private Quote quote;
    private Base base;
  }

  @ToString
  @Getter
  public static class Quote {
    private String amount;
    private String last_cumulative_multiplier_x18;
  }

  @ToString
  @Getter
  public static class Base {
    private String amount;
    private String last_cumulative_multiplier_x18;
  }

  @ToString
  @Getter
  public static class BookInfo {
    private String size_increment;
    private String price_increment_x18;
    private String min_size;
    private String collected_fees;
    private String lp_spread_x18;
  }

  @ToString
  @Getter
  public static class Perp {
    private String product_id;
    private String oracle_price_x18;
    private Risk risk;
    private Config config;
    private PerpState state;
    private LpState lp_state;
    private BookInfo book_info;
  }

  @ToString
  @Getter
  public static class PerpState {
    private String cumulative_funding_long_x18;
    private String cumulative_funding_short_x18;
    private String available_settle;
    private String open_interest;

  }
}
