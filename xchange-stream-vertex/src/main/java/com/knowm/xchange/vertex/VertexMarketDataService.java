package com.knowm.xchange.vertex;

import com.knowm.xchange.vertex.dto.FundingRateRequest;
import com.knowm.xchange.vertex.dto.FundingRateResponse;
import com.knowm.xchange.vertex.dto.ProductSnapshotRequest;
import com.knowm.xchange.vertex.dto.ProductSnapshotResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchange.dto.marketdata.FundingRate;
import org.knowm.xchange.dto.marketdata.FundingRates;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.knowm.xchange.vertex.dto.VertexModelUtils.readX18Decimal;

public class VertexMarketDataService implements MarketDataService {

  private final VertexExchange exchange;
  private final VertexProductInfo productInfo;

  public VertexMarketDataService(VertexExchange exchange, VertexProductInfo productInfo) {
    this.exchange = exchange;
    this.productInfo = productInfo;
  }


  /*
   Returns funding rates for perps and borrow (ihr funding)/deposit(8hr funding) rates for spot
   */
  @Override
  public FundingRates getFundingRates() {

    List<Long> productsIds = productInfo.getProductsIds();
    Long[] perpIds = productsIds.stream().filter(id -> {
      Instrument instrument = productInfo.lookupInstrument(id);
      return !productInfo.isSpot(instrument);
    }).toArray(Long[]::new);

    Map<String, FundingRateResponse> snapshotMap = getFundingRates(perpIds);

    List<FundingRate> perpFunding = snapshotMap.entrySet().stream().map(resp -> {
      long productId = Long.parseLong(resp.getKey());
      FundingRateResponse snapshot = resp.getValue();
      return buildFunding(snapshot, productId);
    }).filter(Objects::nonNull).collect(Collectors.toList());


    Long[] spotIds = productsIds.stream().filter(id -> {
      Instrument instrument = productInfo.lookupInstrument(id);
      return productInfo.isSpot(instrument);
    }).toArray(Long[]::new);

    // add zero product id for USDC
    spotIds = ArrayUtils.add(spotIds, 0L);
    Map<String, ProductSnapshotResponse> productSnapshots = requestProductSnapshots(spotIds);
    List<FundingRate> spotFunding = productSnapshots.entrySet().stream().map(resp -> {
      long productId = Long.parseLong(resp.getKey());
      ProductSnapshotResponse snapshot = resp.getValue();
      return buildInterest(snapshot, productId);
    }).collect(Collectors.toList());


    perpFunding.addAll(spotFunding);
    return new FundingRates(perpFunding);
  }

  private FundingRate buildInterest(ProductSnapshotResponse snapshot, long productId) {
    if (snapshot == null) return null;
    BigDecimal annualBorrowRate = InterestCalculator.calculateAnnualBorrowRate(snapshot.getProduct().getSpot());
    BigDecimal annualDepositRate = InterestCalculator.calcRealizedDepositRateForTimeRange(snapshot.getProduct().getSpot(), BigDecimal.valueOf(InterestCalculator.ONE_YEAR_IN_SECONDS), productInfo.interestFee());

    return new FundingRate(productInfo.lookupInstrument(productId), annualBorrowRate, annualDepositRate, new Date(), 0);
  }

  private Map<String, FundingRateResponse> getFundingRates(Long[] perpIds) {
    FundingRateRequest req = new FundingRateRequest(new FundingRateRequest.FundingRateParams(perpIds));
    return exchange.archiveApi().fundingRates(req);
  }

  @Override
  public FundingRate getFundingRate(Instrument instrument) {
    long productId = productInfo.lookupProductId(instrument);
    Long[] productIds = {productId};
    if (productInfo.isSpot(instrument)) {
      Map<String, ProductSnapshotResponse> productSnapshots = requestProductSnapshots(productIds);
      return buildInterest(productSnapshots.get(String.valueOf(productId)), productId);
    } else {
      Map<String, FundingRateResponse> snapshotMap = getFundingRates(productIds);
      FundingRateResponse snapshot = snapshotMap.get(String.valueOf(productId));
      return buildFunding(snapshot, productId);
    }
  }

  private Map<String, ProductSnapshotResponse> requestProductSnapshots(Long[] productIds) {
    return exchange.archiveApi().productSnapshots(new ProductSnapshotRequest(new ProductSnapshotRequest.ProductSnapshotParams(productIds, null)));
  }

  private FundingRate buildFunding(FundingRateResponse snapshot, long productId) {
    if (snapshot == null) return null;
    BigDecimal funding24 = readX18Decimal(snapshot.getFunding_rate_x18());
    BigDecimal funding1h = funding24.divide(BigDecimal.valueOf(24), 8, RoundingMode.HALF_UP);
    BigDecimal funding8h = funding24.divide(BigDecimal.valueOf(3), 8, RoundingMode.HALF_UP);
    Date fundingTime = new Date();
    // round up to the nearest hour
    fundingTime.setTime(fundingTime.getTime() + 3600000 - fundingTime.getTime() % 3600000);
    return new FundingRate(productInfo.lookupInstrument(productId), funding1h, funding8h, fundingTime, 0);
  }
}
