package com.knowm.xchange.vertex.dto;

import lombok.Getter;
import lombok.ToString;


@ToString
@Getter
public class FundingRateRequest {

  private final FundingRateParams funding_rates;

  public FundingRateRequest(FundingRateParams fundingRates) {
    funding_rates = fundingRates;
  }


  @ToString
  @Getter
  public static class FundingRateParams {

    private final Long[] product_ids;

    public FundingRateParams(Long[] product_ids) {
      this.product_ids = product_ids;
    }
  }
}
