package com.knowm.xchange.vertex;

import com.knowm.xchange.vertex.api.VertexArchiveApi;
import com.knowm.xchange.vertex.api.VertexQueryApi;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.client.ExchangeRestProxyBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VertexExchange extends BaseExchange {

  public static final String GATEWAY_REST = "gatewayRestUrl";
  public static final String ARCHIVER_REST = "archiverRestUrl";
  private VertexArchiveApi archiveApi;
  private VertexQueryApi queryApi;

  private boolean useTestnet;

  public static String overrideOrDefault(String param, String defaultVl, ExchangeSpecification exchangeSpecification1) {
    String override = getParam(param, exchangeSpecification1);
    if (override != null) return override;
    return defaultVl;
  }

  static String getGatewayHost(boolean useTestnet) {
    return useTestnet ? "gateway.sepolia-test.vertexprotocol.com" : "gateway.prod.vertexprotocol.com";
  }


  public void applySpecification(ExchangeSpecification exchangeSpecification) {
    this.useTestnet = Boolean.TRUE.equals(Boolean.parseBoolean(Objects.toString(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX))));

    if (useTestnet) {
      exchangeSpecification.setHost(VertexExchange.getGatewayHost(useTestnet));
    }

    super.applySpecification(exchangeSpecification);
  }


  @Override
  protected void initServices() {

    ExchangeSpecification archiveSpec = new ExchangeSpecification(this.getClass());
    archiveSpec.setSslUri(getArchiveRestUrl());
    this.archiveApi = ExchangeRestProxyBuilder.forInterface(VertexArchiveApi.class, archiveSpec)
        .clientConfigCustomizer(clientConfig -> clientConfig.setHttpReadTimeout((int) TimeUnit.SECONDS.toMillis(60)))
        .clientConfigCustomizer(clientConfig -> clientConfig.setHttpConnTimeout((int) TimeUnit.SECONDS.toMillis(10)))
        .build();


    ExchangeSpecification gatewaySpec = new ExchangeSpecification(this.getClass());
    gatewaySpec.setSslUri(getGatewayRestUrl());
    this.queryApi = ExchangeRestProxyBuilder.forInterface(VertexQueryApi.class, gatewaySpec)
        .clientConfigCustomizer(clientConfig -> {
          clientConfig.setHttpReadTimeout((int) TimeUnit.SECONDS.toMillis(60));
          clientConfig.setHttpConnTimeout((int) TimeUnit.SECONDS.toMillis(10));
          clientConfig.setHostnameVerifier((s, sslSession) -> true);
        })
        .build();

  }


  private static String getArchiveHost(boolean useTestnet) {
    return useTestnet ? "archive.sepolia-test.vertexprotocol.com" : "archive.prod.vertexprotocol.com";
  }


  String getGatewayRestUrl() {
    return overrideOrDefault(GATEWAY_REST, "https://" + getGatewayHost(useTestnet) + "/v1", exchangeSpecification);
  }

  String getArchiveRestUrl() {
    return overrideOrDefault(ARCHIVER_REST, "https://" + getArchiveHost(useTestnet) + "/v1", exchangeSpecification);

  }

  private static String getParam(String param, ExchangeSpecification exchangeSpecification1) {
    Object exchangeSpecificParametersItem1 = exchangeSpecification1.getExchangeSpecificParametersItem(param);
    if (exchangeSpecificParametersItem1 != null) {
      String override = String.valueOf(exchangeSpecificParametersItem1);
      return StringUtils.isNotEmpty(override) ? override : null;
    }
    return null;
  }


  @Override
  public ExchangeSpecification getDefaultExchangeSpecification() {
    ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass());
    exchangeSpecification.setHost(VertexExchange.getGatewayHost(useTestnet));
    exchangeSpecification.setExchangeName("Vertex");
    exchangeSpecification.setExchangeDescription("Vertex - One DEX. Everything you need.");
    return exchangeSpecification;
  }

  public VertexQueryApi queryAPI() {
    return queryApi;
  }

  public VertexArchiveApi archiveApi() {
    return archiveApi;
  }
}
