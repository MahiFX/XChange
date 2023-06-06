package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.ConnectionStateModel;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.trade.TradeService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.convertToDecimal;

public class VertexStreamingExchange extends BaseExchange implements StreamingExchange {

    public static final String USE_LEVERAGE = "useLeverage";
    public static final String MAX_SLIPPAGE_RATIO = "maxSlippageRatio";

    private static final String WS_TESTNET_API_URL = "wss://test.vertexprotocol-backend.com";
    private static final String WS_API_URL = "wss://prod.vertexprotocol-backend.com";
    private VertexStreamingService subscriptionStream;
    private VertexStreamingMarketDataService streamingMarketDataService;
    private VertexStreamingTradeService streamingTradeService;

    private boolean useTestnet;

    private long chainId;

    private String endpointContract;

    private String bookContract;

    private VertexStreamingService requestResponseStream;
    private VertexProductInfo productInfo;

    private final Map<Long, TopOfBookPrice> marketPrices = new ConcurrentHashMap<>();
    private final Map<Long, InstrumentDefinition> increments = new HashMap<>();


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


    public void applySpecification(ExchangeSpecification exchangeSpecification) {
        this.useTestnet = !Boolean.FALSE.equals(exchangeSpecification.getExchangeSpecificParametersItem(USE_SANDBOX));
        super.applySpecification(exchangeSpecification);
    }


    @Override
    public void remoteInit() throws ExchangeException {

        if (!requestResponseStream.isSocketOpen() && !requestResponseStream.connect().blockingAwait(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for connection");
        }

        ArrayList<Query> queries = new ArrayList<>();
        logger.info("Loading contract data and current prices");
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
                        marketPrices.computeIfAbsent(productId, k -> new TopOfBookPrice(convertToDecimal(bid), convertToDecimal(offer)));
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
                        increments.put(productId, new InstrumentDefinition(priceIncrement, quantityIncrement));
                    }
                }));

        submitQueries(queries.toArray(new Query[0]));


    }

    public synchronized void submitQueries(Query... queries) {

        Observable<JsonNode> stream = requestResponseStream.subscribeChannel(ALL_MESSAGES);

        for (Query query : queries) {
            CountDownLatch responseLatch = new CountDownLatch(1);
            Disposable subscription = stream.subscribe(resp -> {
                JsonNode data = resp.get("data");
                if (data != null) {
                    query.getRespHandler().accept(data);
                    logger.info("Query response " + data.toPrettyString());
                }
                responseLatch.countDown();
            }, (err) -> logger.error("Query error running " + query.getQueryMsg(), err));
            try {
                logger.info("Sending query " + query.getQueryMsg());
                requestResponseStream.sendMessage(query.getQueryMsg());
                if (!responseLatch.await(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timed out waiting for response for " + query.getQueryMsg());
                }
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

        this.subscriptionStream = createStreamingService("/subscribe");
        this.requestResponseStream = createStreamingService("/ws");

    }


    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        if (this.streamingMarketDataService == null) {
            this.streamingMarketDataService = new VertexStreamingMarketDataService(subscriptionStream, productInfo, this);
        }
        return streamingMarketDataService;
    }

    @Override
    public VertexStreamingTradeService getStreamingTradeService() {
        if (this.streamingTradeService == null) {
            this.streamingTradeService = new VertexStreamingTradeService(requestResponseStream, getExchangeSpecification(), productInfo, chainId, bookContract, this, endpointContract, getStreamingMarketDataService());
        }
        return streamingTradeService;
    }

    @Override
    public TradeService getTradeService() {
        return streamingTradeService;
    }

    @Override
    public Observable<ConnectionStateModel.State> connectionStateObservable() {
        return subscriptionStream.subscribeConnectionState();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return subscriptionStream.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return subscriptionStream.subscribeConnectionSuccess();
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return Completable.concatArray(subscriptionStream.connect(), requestResponseStream.connect());
    }

    @Override
    public Completable disconnect() {
        return Completable.concatArray(subscriptionStream.disconnect(), requestResponseStream.disconnect());
    }

    @Override
    public boolean isAlive() {
        return subscriptionStream.isSocketOpen() && requestResponseStream.isSocketOpen();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        subscriptionStream.useCompressedMessages(compressedMessages);
    }

    public TopOfBookPrice getMarketPrice(Long productId) {
        return marketPrices.get(productId);
    }

    public void setMarketPrice(Long productId, TopOfBookPrice price) {
        marketPrices.put(productId, price);
    }


    /**
     * size and price increments for a product
     */
    public InstrumentDefinition getIncrements(Long productId) {
        return increments.get(productId);
    }
}
