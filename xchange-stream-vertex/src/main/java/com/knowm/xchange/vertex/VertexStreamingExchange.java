package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.TradeService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.convertToDecimal;

public class VertexStreamingExchange extends BaseExchange implements StreamingExchange {

    private static final String WS_TESTNET_API_URL = "wss://test.vertexprotocol-backend.com";
    private static final String WS_API_URL = "wss://prod.vertexprotocol-backend.com";
    private VertexStreamingService marketDataStreamService;
    private VertexStreamingMarketDataService streamingMarketDataService;
    private VertexStreamingTradeService streamingTradeService;

    private boolean useTestnet;

    private long chainId;

    private String endpointContract;

    private String bookContract;

    private VertexStreamingService orderStreamService;
    private VertexProductInfo productInfo;

    private Map<Long, Pair<BigDecimal, BigDecimal>> marketPrices = new HashMap<>();
    private Map<Long, Pair<BigDecimal, BigDecimal>> increments = new HashMap<>();


    private VertexStreamingService createStreamingService(String suffix) {
        VertexStreamingService streamingService = new VertexStreamingService(getApiUrl() + suffix);
        applyStreamingSpecification(getExchangeSpecification(), streamingService);

        return streamingService;
    }

    private String getApiUrl() {
        if (useTestnet) {
            return WS_TESTNET_API_URL;
        } else {
            return WS_API_URL;
        }
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass());
        exchangeSpecification.setSslUri("https://prod.vertexprotocol-backend.com");
        exchangeSpecification.setHost("prod.vertexprotocol-backend.com");
        exchangeSpecification.setExchangeName("Vertex");
        exchangeSpecification.setExchangeDescription("Vertex - One DEX. Everything you need.");
        return exchangeSpecification;
    }

    @Override
    public void applyStreamingSpecification(ExchangeSpecification exchangeSpec, NettyStreamingService<?> streamingService) {
        StreamingExchange.super.applyStreamingSpecification(exchangeSpec, streamingService);
        this.useTestnet = !Boolean.FALSE.equals(exchangeSpec.getExchangeSpecificParametersItem(USE_SANDBOX));
    }

    @Override
    public void remoteInit() throws ExchangeException {


        orderStreamService.connect().blockingAwait();

        ArrayList<Query> queries = new ArrayList<>();

        queries.add(new Query("{\"type\":\"contracts\"}",
                data1 -> {
                    chainId = Long.parseLong(data1.get("chain_id").asText());
                    endpointContract = data1.get("endpoint_addr").asText();
                    bookContract = data1.withArray("book_addrs").get(1).asText();
                }));

        for (Long productId : productInfo.getProductsIds()) {
            Query marketPricesQuery = new Query("{\"type\":\"market_price\", \"product_id\": " + productId + "}",
                    data -> {
                        JsonNode bidX18 = data.get("bid_x18");
                        BigInteger bid = new BigInteger(bidX18.asText());
                        JsonNode offerX18 = data.get("ask_x18");
                        BigInteger offer = new BigInteger(offerX18.asText());
                        marketPrices.computeIfAbsent(productId, k -> Pair.of(convertToDecimal(bid), convertToDecimal(offer)));
                    });
            queries.add(marketPricesQuery);
        }

        queries.add(new Query("{\"type\":\"all_products\"}",
                data -> {
                    ArrayNode spotProducts = data.withArray("spot_products");
                    for (JsonNode spotProduct : spotProducts) {
                        long productId = spotProduct.get("product_id").asLong();
                        if (productId == 0) { // skip USDC product
                            continue;
                        }
                        JsonNode bookInfo = spotProduct.get("book_info");
                        BigDecimal quantityIncrement = convertToDecimal(new BigInteger(bookInfo.get("size_increment").asText()));
                        BigDecimal priceIncrement = convertToDecimal(new BigInteger(bookInfo.get("price_increment_x18").asText()));
                        increments.put(productId, Pair.of(quantityIncrement, priceIncrement));
                    }
                }));

        submitQueries(queries.toArray(new Query[0]));


    }

    private void submitQueries(Query... queries) {

        Observable<JsonNode> stream = orderStreamService.subscribeChannel(ALL_MESSAGES);

        for (int i = 0; i < queries.length; i++) {
            Query query = queries[i];
            CountDownLatch responseLatch = new CountDownLatch(1);
            Disposable subscription = stream.subscribe(resp -> {
                JsonNode data = resp.get("data");
                if (data != null) {
                    query.getRespHandler().accept(data);
                }
                responseLatch.countDown();
            }, (err) -> logger.error("Query error running " + query.getQueryMsg(), err));
            try {
                orderStreamService.sendMessage(query.getQueryMsg());
                responseLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Failed to get contract data due to timeout");
            } finally {
                subscription.dispose();
            }
        }


    }

    @Override
    protected void initServices() {

        productInfo = new VertexProductInfo();

        this.marketDataStreamService = createStreamingService("/subscribe");
        this.orderStreamService = createStreamingService("/ws");

    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        if (this.streamingMarketDataService == null) {
            this.streamingMarketDataService = new VertexStreamingMarketDataService(marketDataStreamService, productInfo);
        }
        return streamingMarketDataService;
    }

    @Override
    public VertexStreamingTradeService getStreamingTradeService() {
        if (this.streamingTradeService == null) {
            this.streamingTradeService = new VertexStreamingTradeService(orderStreamService, getExchangeSpecification(), productInfo, chainId, bookContract, this, endpointContract);
        }
        return streamingTradeService;
    }

    @Override
    public TradeService getTradeService() {
        return streamingTradeService;
    }

    @Override
    public Observable<ConnectionStateModel.State> connectionStateObservable() {
        return marketDataStreamService.subscribeConnectionState();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return marketDataStreamService.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return marketDataStreamService.subscribeConnectionSuccess();
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return marketDataStreamService.connect();
    }

    @Override
    public Completable disconnect() {
        return marketDataStreamService.disconnect();
    }

    @Override
    public boolean isAlive() {
        return marketDataStreamService.isSocketOpen();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        marketDataStreamService.useCompressedMessages(compressedMessages);
    }

    public Pair<BigDecimal, BigDecimal> getMarketPrice(Long productId) {
        return marketPrices.get(productId);
    }

    /**
     * size and price increments for a product
     *
     * @param productId
     * @return
     */
    public Pair<BigDecimal, BigDecimal> getIncrements(Long productId) {
        return increments.get(productId);
    }
}
