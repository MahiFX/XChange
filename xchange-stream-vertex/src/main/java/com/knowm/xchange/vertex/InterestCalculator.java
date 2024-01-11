package com.knowm.xchange.vertex;

import com.knowm.xchange.vertex.dto.ProductSnapshotResponse;
import com.knowm.xchange.vertex.dto.VertexModelUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InterestCalculator {

  public static final int ONE_YEAR_IN_SECONDS = 31536000;

  // implements https://github.com/vertex-protocol/vertex-typescript-sdk/blob/main/packages/contracts/src/utils/interest.ts  in Java


  /**
   * Calculates utilization ratio = abs(total borrowed / total deposited)
   */
  private static BigDecimal calculateUtilizationRatio(ProductSnapshotResponse.Spot spot) {
    BigDecimal totalBorrowed = calcTotalBorrowed(spot);
    BigDecimal totalDeposited = calcTotalDeposited(spot);
    return totalBorrowed.abs().divide(totalDeposited.abs(), 18, RoundingMode.HALF_UP);
  }


  /**
   * Calculate amount total borrowed for a product.
   */
  private static BigDecimal calcTotalBorrowed(ProductSnapshotResponse.Spot spot) {
    BigDecimal totalBorrowed = VertexModelUtils.readX18Decimal(spot.getState().getTotal_borrows_normalized());
    BigDecimal cumulativeBorrowsMultiplierX18 = VertexModelUtils.readX18Decimal(spot.getState().getCumulative_borrows_multiplier_x18());
    return totalBorrowed.multiply(cumulativeBorrowsMultiplierX18);
  }

  /**
   * Calculate amount total deposited for a product.
   */
  private static BigDecimal calcTotalDeposited(ProductSnapshotResponse.Spot spot) {
    BigDecimal totalDeposited = VertexModelUtils.readX18Decimal(spot.getState().getTotal_deposits_normalized());
    BigDecimal cumulativeDepositsMultiplierX18 = VertexModelUtils.readX18Decimal(spot.getState().getCumulative_deposits_multiplier_x18());
    return totalDeposited.multiply(cumulativeDepositsMultiplierX18);
  }

  /*


   * Calculates per-second borrow interest rate for a product. For example, a returned rate of 0.1 indicates 10% borrower
   * interest. The calculation for interest rate is as follows:
   *
   * If utilization ratio > inflection:
   *  annual rate = (1 - utilization ratio) / (1 - inflection) * interestLargeCap + interestFloor + interestSmallCap
   *
   * If utilization ratio < inflection:
   *  annual rate = utilization * interestSmallCap / inflection + utilization
   *
   * The returned rate is annual rate / 31536000 seconds per year.
   *
   * {@label UTILS}
   * @param product Spot product
   */
  private static BigDecimal calcBorrowRatePerSecond(ProductSnapshotResponse.Spot spot) {

    BigDecimal annualRate = calculateAnnualBorrowRate(spot);
    return annualRate.divide(BigDecimal.valueOf(ONE_YEAR_IN_SECONDS), 18, RoundingMode.HALF_UP);
  }

  public static BigDecimal calculateAnnualBorrowRate(ProductSnapshotResponse.Spot spot) {

    BigDecimal utilization = calculateUtilizationRatio(spot);
    if (utilization.equals(BigDecimal.ZERO)) {
      return BigDecimal.ZERO;
    }
    ProductSnapshotResponse.Config productConfig = spot.getConfig();

    BigDecimal interestInflection = VertexModelUtils.readX18Decimal(productConfig.getInterest_inflection_util_x18());
    boolean pastInflection = utilization.compareTo(interestInflection) > 0;
    BigDecimal annualRate;
    if (pastInflection) {
      BigDecimal utilizationTerm = VertexModelUtils.readX18Decimal(productConfig.getInterest_large_cap_x18()).multiply(utilization.subtract(interestInflection)).divide(BigDecimal.ONE.subtract(interestInflection), 18, RoundingMode.HALF_UP);
      annualRate = VertexModelUtils.readX18Decimal(productConfig.getInterest_floor_x18()).add(VertexModelUtils.readX18Decimal(productConfig.getInterest_small_cap_x18())).add(utilizationTerm);
    } else {
      BigDecimal utilizationTerm = utilization.divide(interestInflection, 18, RoundingMode.HALF_UP).multiply(VertexModelUtils.readX18Decimal(productConfig.getInterest_small_cap_x18()));
      annualRate = VertexModelUtils.readX18Decimal(productConfig.getInterest_floor_x18()).add(utilizationTerm);
    }
    return annualRate;
  }


  public static BigDecimal calcBorrowRateForTimeRange(BigDecimal seconds, ProductSnapshotResponse.Spot spot) {
    BigDecimal borrowRatePerSecond = calcBorrowRatePerSecond(spot);
    return BigDecimal.valueOf(Math.pow(borrowRatePerSecond.doubleValue() + 1, seconds.doubleValue()) - 1);
  }


  public static BigDecimal calcRealizedDepositRateForTimeRange(ProductSnapshotResponse.Spot spot, BigDecimal seconds, BigDecimal interestFeeFrac) {
    BigDecimal utilization = calculateUtilizationRatio(spot);
    if (utilization.equals(BigDecimal.ZERO)) {
      return BigDecimal.ZERO;
    }
    return utilization.multiply(calcBorrowRateForTimeRange(seconds, spot)).multiply(BigDecimal.ONE.subtract(interestFeeFrac));
  }

}
