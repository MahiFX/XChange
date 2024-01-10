package com.knowm.xchange.vertex;

import com.knowm.xchange.vertex.dto.FundingRateRequest;
import com.knowm.xchange.vertex.dto.FundingRateResponse;
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


  @Override
  public FundingRates getFundingRates() {

    List<Long> productsIds = productInfo.getProductsIds();
    Long[] perpIds = productsIds.stream().filter(id -> {
      Instrument instrument = productInfo.lookupInstrument(id);
      return !productInfo.isSpot(instrument);
    }).toArray(Long[]::new);


    FundingRateRequest req = new FundingRateRequest(new FundingRateRequest.FundingRateParams(perpIds));
    Map<String, FundingRateResponse> snapshotMap = exchange.archiveApi().fundingRates(req);
    return new FundingRates(snapshotMap.entrySet().stream().map(resp -> {
      long productId = Long.parseLong(resp.getKey());
      FundingRateResponse snapshot = resp.getValue();
      return buildFunding(snapshot, productId);
    }).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  @Override
  public FundingRate getFundingRate(Instrument instrument) {
    long productId = productInfo.lookupProductId(instrument);
    FundingRateRequest req = new FundingRateRequest(new FundingRateRequest.FundingRateParams(new Long[]{productId}));
    Map<String, FundingRateResponse> snapshotMap = exchange.archiveApi().fundingRates(req);
    FundingRateResponse snapshot = snapshotMap.get(String.valueOf(productId));
    return buildFunding(snapshot, productId);
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
