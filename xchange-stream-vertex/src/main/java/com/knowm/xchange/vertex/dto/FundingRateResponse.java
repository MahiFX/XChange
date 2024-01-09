package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class FundingRateResponse {

  private String product_id;
  private String funding_rate_x18;
  private String update_time;

}
