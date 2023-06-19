package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.knowm.xchange.vertex.dto.*;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.SignatureAndDigest;
import com.knowm.xchange.vertex.signing.schemas.CancelOrdersSchema;
import com.knowm.xchange.vertex.signing.schemas.CancelProductOrdersSchema;
import com.knowm.xchange.vertex.signing.schemas.PlaceOrderSchema;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.instrument.Instrument;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.*;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParamInstrument;
import org.knowm.xchange.service.trade.params.orders.OpenOrdersParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.knowm.xchange.vertex.VertexStreamingExchange.MAX_SLIPPAGE_RATIO;
import static com.knowm.xchange.vertex.VertexStreamingExchange.USE_LEVERAGE;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.*;

public class VertexStreamingTradeService implements StreamingTradeService, TradeService {

    public static final double DEFAULT_MAX_SLIPPAGE_RATIO = 0.005;
    private static final boolean DEFAULT_USE_LEVERAGE = false;
    public static final Consumer<Ticker> NO_OP = ticker -> {
    };
    public static final String DEFAULT_SUB_ACCOUNT = "default";
    private final Logger logger = LoggerFactory.getLogger(VertexStreamingTradeService.class);

    private final VertexStreamingService requestResponseStream;
    private final VertexStreamingService subscriptionStream;
    private final ExchangeSpecification exchangeSpecification;
    private final ObjectMapper mapper;

    private final VertexProductInfo productInfo;
    private final long chainId;
    private final List<String> bookContracts;
    private final VertexStreamingExchange exchange;
    private final String endpointContract;
    private final double slippage;
    private final boolean useLeverage;
    private final Map<String, CountDownLatch> responseLatches = new ConcurrentHashMap<>();
    private final Map<Long, Disposable> tickerSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Observable<JsonNode>> fillSubscriptions = new ConcurrentHashMap<>();
    private final Disposable allMessageSubscription;
    private final AtomicReference<Throwable> errorHolder = new AtomicReference<>();
    private final StreamingMarketDataService marketDataService;

    public VertexStreamingTradeService(VertexStreamingService requestResponseStream, VertexStreamingService subscriptionStream, ExchangeSpecification exchangeSpecification, VertexProductInfo productInfo, long chainId, List<String> bookContracts, VertexStreamingExchange exchange, String endpointContract, StreamingMarketDataService marketDataService) {
        this.requestResponseStream = requestResponseStream;
        this.subscriptionStream = subscriptionStream;
        this.exchangeSpecification = exchangeSpecification;
        this.productInfo = productInfo;
        this.chainId = chainId;
        this.bookContracts = bookContracts;
        this.endpointContract = endpointContract;
        this.exchange = exchange;
        this.marketDataService = marketDataService;
        this.mapper = StreamingObjectMapperHelper.getObjectMapper();
        this.slippage = exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO) != null ? Double.parseDouble(exchangeSpecification.getExchangeSpecificParametersItem(MAX_SLIPPAGE_RATIO).toString()) : DEFAULT_MAX_SLIPPAGE_RATIO;
        this.useLeverage = exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE) != null ? Boolean.parseBoolean(exchangeSpecification.getExchangeSpecificParametersItem(USE_LEVERAGE).toString()) : DEFAULT_USE_LEVERAGE;

        this.allMessageSubscription = exchange.subscribeToAllMessages().subscribe(resp -> {
            if (resp.get("status").asText().equals("success")) {
                logger.info("Received response: {}", resp);
            } else {
                logger.error("Received error: {}", resp);
                errorHolder.set(new RuntimeException("Websocket message error: " + resp.get("error").asText()));
            }
            JsonNode respSignature = resp.get("signature");
            if (respSignature != null) {
                CountDownLatch replyLatch = responseLatches.remove(respSignature.asText());
                if (replyLatch != null) {
                    replyLatch.countDown();
                }
            }
        });

    }

    public void disconnect() {
        allMessageSubscription.dispose();
        tickerSubscriptions.values().stream().filter(Disposable::isDisposed).forEach(Disposable::dispose);
        if (requestResponseStream.isSocketOpen()) {
            if (!requestResponseStream.disconnect().blockingAwait(10, TimeUnit.SECONDS)) {
                logger.warn("Timeout waiting for disconnect");
            }
        }
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {

        BigDecimal price = limitOrder.getLimitPrice();

        return placeOrder(limitOrder, price);
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {

        long productId = productInfo.lookupProductId(marketOrder.getInstrument());

        BigDecimal price = getPrice(marketOrder, productId);

        return placeOrder(marketOrder, price);
    }


    @Override
    public Observable<Order> getOrderChanges(Instrument instrument, Object... args) {
        return subscribeToFills(instrument).map(resp -> {
            boolean isBid = resp.get("is_bid").asBoolean();
            Order.Builder builder = new LimitOrder.Builder(isBid ? Order.OrderType.BID : Order.OrderType.ASK, instrument);
            String orderId = resp.get("order_digest").asText();
            BigDecimal original = readX18Decimal(resp, "original_qty");
            BigDecimal remaining = readX18Decimal(resp, "remaining_qty");
            BigDecimal price = readX18Decimal(resp, "price");
            BigDecimal filled = readX18Decimal(resp, "filled_qty");
            Instant timestamp = NanoSecondsDeserializer.parse(resp.get("timestamp").asText());
            String respSubAccount = resp.get("subaccount").asText();
            Order.OrderStatus status = getOrderStatus(remaining, filled);
            return builder.id(orderId)
                    .instrument(instrument)
                    .originalAmount(original)
                    .cumulativeAmount(filled)
                    .orderStatus(status)
                    .averagePrice(price)
                    .remainingAmount(remaining)
                    .userReference(respSubAccount)
                    .timestamp(new Date(timestamp.toEpochMilli()))
                    .build();
        });
    }

    private static Order.OrderStatus getOrderStatus(BigDecimal remaining, BigDecimal filled) {
        Order.OrderStatus status;
        if (remaining.equals(BigDecimal.ZERO)) {
            status = Order.OrderStatus.FILLED;
        } else if (filled.equals(BigDecimal.ZERO)) {
            status = Order.OrderStatus.NEW;
        } else {
            status = Order.OrderStatus.PARTIALLY_FILLED;
        }
        return status;
    }

    private Observable<JsonNode> subscribeToFills(Instrument instrument) {
        long productId = productInfo.lookupProductId(instrument);

        String subAccount = getSubAccountOrDefault();

        String channel = "fill." + productId + "." + buildSender(exchangeSpecification.getApiKey(), subAccount);
        return fillSubscriptions.computeIfAbsent(channel, c -> subscriptionStream.subscribeChannel(channel));
    }


    @Override
    public Observable<UserTrade> getUserTrades(Instrument instrument, Object... args) {

        return subscribeToFills(instrument).map(resp -> {
                    boolean isBid = resp.get("is_bid").asBoolean();
                    UserTrade.Builder builder = new UserTrade.Builder();

                    String orderId = resp.get("order_digest").asText();
                    BigDecimal price = readX18Decimal(resp, "price");
                    BigDecimal filled = readX18Decimal(resp, "filled_qty");
                    if (filled.equals(BigDecimal.ZERO)) {
                        return Optional.<UserTrade>empty();
                    }
                    String timestampText = resp.get("timestamp").asText();
                    Instant timestamp = NanoSecondsDeserializer.parse(timestampText);
                    String respSubAccount = resp.get("subaccount").asText();

                    return Optional.of(builder.id(timestampText)
                            .instrument(instrument)
                            .originalAmount(filled)
                            .orderId(orderId)
                            .price(price)
                            .type(isBid ? Order.OrderType.BID : Order.OrderType.ASK)
                            .orderUserReference(respSubAccount)
                            .timestamp(new Date(timestamp.toEpochMilli()))
                            .creationTimestamp(new Date())
                            .build());
                })
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private String placeOrder(Order marketOrder, BigDecimal price) throws IOException {

        Instrument instrument = marketOrder.getInstrument();
        long productId = productInfo.lookupProductId(instrument);

        BigInteger expiration = getExpiration(marketOrder.getOrderFlags());

        InstrumentDefinition increments = exchange.getIncrements(productId);
        BigDecimal priceIncrement = increments.getPriceIncrement();
        price = roundToIncrement(price, priceIncrement);
        BigInteger priceAsInt = convertToInteger(price);

        BigDecimal quantity = getQuantity(marketOrder);
        BigDecimal quantityIncrement = increments.getQuantityIncrement();
        if (quantity.abs().compareTo(quantityIncrement) < 0) {
            throw new IllegalArgumentException("Quantity must be greater than increment");
        }
        quantity = roundToIncrement(quantity, quantityIncrement);
        BigInteger quantityAsInt = convertToInteger(quantity);


        String subAccount = getSubAccountOrDefault();
        String nonce = buildNonce(60000);
        String walletAddress = exchangeSpecification.getApiKey();
        String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

        String bookContract = bookContracts.get((int) productId);
        PlaceOrderSchema orderSchema = PlaceOrderSchema.build(chainId,
                bookContract,
                Long.valueOf(nonce),
                sender,
                expiration,
                quantityAsInt,
                priceAsInt);
        SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

        VertexPlaceOrderMessage orderMessage = new VertexPlaceOrderMessage(new VertexPlaceOrder(
                productId,
                new VertexOrder(sender, priceAsInt.toString(), quantityAsInt.toString(), expiration.toString(), nonce),
                signatureAndDigest.getSignature(),
                productInfo.isSpot(instrument) ? useLeverage : null));


        Optional<Throwable> sendError = sendWebsocketMessage(orderMessage);
        if (sendError.isPresent()) {
            Throwable throwable = sendError.get();
            logger.error("Failed to place order : " + orderMessage, throwable);
            throw new ExchangeException(throwable.getMessage());
        }

        return signatureAndDigest.getDigest();
    }

    private Optional<Throwable> sendWebsocketMessage(VertexRequest messageObj) throws IOException {

        String message = mapper.writeValueAsString(messageObj);
        logger.info("Sending order: {}", message);
        try {
            CountDownLatch replyLatch = responseLatches.computeIfAbsent(messageObj.getSignature(), s -> new CountDownLatch(1));
            requestResponseStream.sendMessage(message);

            if (!replyLatch.await(5000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw new IOException("Timed out waiting for response");
            }
            Throwable error = errorHolder.get();
            if (error != null) {
                errorHolder.set(null);
                return Optional.of(error);
            }
        } catch (InterruptedException ignored) {
        }
        return Optional.empty();

    }

    private BigInteger getExpiration(Set<Order.IOrderFlags> orderFlags) {
        BigInteger timeInForce = BigInteger.ZERO; // resting
        Instant expiryTime = Instant.MAX; // No expiry
        if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_IOC)) {
            timeInForce = BigInteger.ONE;
            expiryTime = Instant.now().plus(5, ChronoUnit.SECONDS); // Force IOC/FOK timeouts
        } else if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_FOK)) {
            timeInForce = BigInteger.valueOf(2);
            expiryTime = Instant.now().plus(5, ChronoUnit.SECONDS); // Force IOC/FOK timeouts
        } else if (orderFlags.contains(VertexOrderFlags.TIME_IN_FORCE_POS_ONLY)) {
            timeInForce = BigInteger.valueOf(3);
        }

        BigInteger expiry = BigInteger.valueOf(expiryTime.getEpochSecond());
        BigInteger tifMask = timeInForce.shiftLeft(62);
        return expiry.or(tifMask);
    }


    private BigDecimal getPrice(Order order, long productId) {
        BigDecimal price;
        if (order instanceof LimitOrder) {
            price = ((LimitOrder) order).getLimitPrice();
        } else {
            // Make sure we have a subscription to the ticker for market prices
            tickerSubscriptions.computeIfAbsent(productId, id -> marketDataService.getTicker(order.getInstrument()).forEach(NO_OP));
            TopOfBookPrice bidOffer = exchange.getMarketPrice(productId);
            boolean isSell = order.getType().equals(Order.OrderType.ASK);
            if (isSell) {
                BigDecimal bid = bidOffer.getBid();
                // subtract max slippage from bid
                price = bid.subtract(bid.multiply(BigDecimal.valueOf(slippage)));
            } else {
                BigDecimal offer = bidOffer.getOffer();
                // add max slippage to offer
                price = offer.add(offer.multiply(BigDecimal.valueOf(slippage)));
            }
        }
        return price;
    }

    @Override
    public Collection<String> cancelAllOrders(CancelAllOrders orderParams) throws IOException {
        cancelOrder(orderParams);
        return Collections.emptyList();
    }

    @Override
    public boolean cancelOrder(CancelOrderParams params) throws IOException {

        String id = getOrderId(params);
        Instrument instrument = getInstrument(params);

        if (StringUtils.isNotEmpty(id) && instrument != null) {

            long productId = productInfo.lookupProductId(instrument);

            String subAccount = getSubAccountOrDefault();
            String nonce = buildNonce(60000);
            String walletAddress = exchangeSpecification.getApiKey();
            String sender = VertexModelUtils.buildSender(walletAddress, subAccount);
            long[] productIds = {productId};
            String[] digests = {id};

            CancelOrdersSchema orderSchema = CancelOrdersSchema.build(chainId, endpointContract, Long.valueOf(nonce), sender, productIds, digests);
            SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

            VertexCancelOrdersMessage orderMessage = new VertexCancelOrdersMessage(new CancelOrders(
                    new Tx(sender, productIds, digests, nonce),
                    signatureAndDigest.getSignature()
            ));

            Optional<Throwable> sendError = sendWebsocketMessage(orderMessage);
            sendError.ifPresent(throwable -> logger.error("Failed to cancel order " + orderMessage, throwable));
            return sendError.isEmpty() || isAlreadyCancelled(sendError.get());

        } else if (params instanceof CancelAllOrders || instrument != null) {
            List<Long> productIds = new ArrayList<>();
            if (instrument != null) {
                productIds.add(productInfo.lookupProductId(instrument));
            }

            String subAccount = getSubAccountOrDefault();
            String nonce = buildNonce(60000);
            String walletAddress = exchangeSpecification.getApiKey();
            String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

            long[] productIdsArray = productIds.stream().mapToLong(l -> l).toArray();

            CancelProductOrdersSchema cancelAllSchema = CancelProductOrdersSchema.build(chainId, endpointContract, Long.valueOf(nonce), sender, productIdsArray);

            SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(cancelAllSchema);

            VertexCancelProductOrdersMessage orderMessage = new VertexCancelProductOrdersMessage(new CancelProductOrders(
                    new Tx(sender, productIdsArray, null, nonce),
                    signatureAndDigest.getSignature()
            ));


            Optional<Throwable> sendError = sendWebsocketMessage(orderMessage);
            sendError.ifPresent(throwable -> logger.error("Failed to cancel order " + orderMessage, throwable));
            return sendError.isEmpty();

        }
        throw new IllegalArgumentException(
                "CancelOrderParams must implement some of CancelOrderByIdParams, CancelOrderByInstrument, CancelOrderByCurrencyPair, CancelAllOrders interfaces.");
    }

    private boolean isAlreadyCancelled(Throwable throwable) {
        return throwable.getMessage().matches(".*Order with the provided digest .* could not be found.*");
    }

    private String getOrderId(CancelOrderParams params) {
        if (params instanceof CancelOrderByIdParams) {
            return ((CancelOrderByIdParams) params).getOrderId();
        }
        return null;
    }

    private Instrument getInstrument(CancelOrderParams params) {
        if (params instanceof CancelOrderByCurrencyPair || params instanceof CancelOrderByInstrument) {
            return params instanceof CancelOrderByCurrencyPair ? ((CancelOrderByCurrencyPair) params).getCurrencyPair() : ((CancelOrderByInstrument) params).getInstrument();
        }
        return null;
    }

    private String getSubAccountOrDefault() {
        return MoreObjects.firstNonNull(exchangeSpecification.getUserName(), DEFAULT_SUB_ACCOUNT);
    }

    @Override
    public OpenOrders getOpenOrders(OpenOrdersParams params) throws IOException {
        if (params instanceof OpenOrdersParamInstrument) {
            CurrencyPair instrument = (CurrencyPair) ((OpenOrdersParamInstrument) params).getInstrument();
            long productId = productInfo.lookupProductId(instrument);
            CountDownLatch response = new CountDownLatch(1);
            AtomicReference<OpenOrders> responseHolder = new AtomicReference<>();
            String subAccount = getSubAccountOrDefault();
            exchange.submitQueries(new Query(openOrders(productId, subAccount), (data) -> {
                List<LimitOrder> orders = new ArrayList<>();
                data.withArray("orders").elements().forEachRemaining(order -> {
                    String priceX18 = "price_x18";
                    BigDecimal price = readX18Decimal(order, priceX18);
                    BigDecimal amount = readX18Decimal(order, "amount");
                    BigDecimal unfilledAmount = readX18Decimal(order, "unfilled_amount");

                    Date placedAt = new Date(Instant.ofEpochSecond(order.get("placed_at").asLong()).toEpochMilli());
                    BigDecimal filled = amount.subtract(unfilledAmount);
                    LimitOrder.Builder builder = new LimitOrder.Builder(amount.compareTo(BigDecimal.ZERO) > 0 ? Order.OrderType.BID : Order.OrderType.ASK, instrument)
                            .id(order.get("digest").asText())
                            .limitPrice(price)
                            .originalAmount(amount)
                            .remainingAmount(unfilledAmount)
                            .orderStatus(getOrderStatus(unfilledAmount, filled))
                            .cumulativeAmount(filled)
                            .timestamp(placedAt);
                    orders.add(builder.build());

                });
                responseHolder.set(new OpenOrders(orders));
                response.countDown();
            }));
            try {
                if (!response.await(10, TimeUnit.SECONDS)) {
                    throw new IOException("Timeout waiting for open orders response");
                }

                return responseHolder.get();
            } catch (InterruptedException ignored) {
                return new OpenOrders(Collections.emptyList());
            }
        } else {
            throw new IllegalArgumentException("Only OpenOrdersParamInstrument is supported");
        }

    }

    private static BigDecimal readX18Decimal(JsonNode obj, String fieldName) {
        return convertToDecimal(new BigInteger(obj.get(fieldName).asText()));
    }

    private String openOrders(long productId, String subAccount) {
        String sender = buildSender(exchangeSpecification.getApiKey(), subAccount);
        return "{\n" +
                "  \"type\": \"subaccount_orders\",\n" +
                "  \"sender\": \"" + sender + "\",\n" +
                "  \"product_id\": " + productId + "\n" +
                "}";
    }

    private BigDecimal getQuantity(Order order) {
        BigDecimal quantityAsInt = order.getOriginalAmount();
        if (order.getType().equals(Order.OrderType.ASK)) {
            quantityAsInt = quantityAsInt.multiply(BigDecimal.valueOf(-1));
        }
        return quantityAsInt;
    }

    public static BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
        if (increment.equals(BigDecimal.ZERO)) return value;
        BigDecimal divided = value.divide(increment, 0, RoundingMode.FLOOR);
        return divided.multiply(increment);
    }


    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        return getOrderChanges((Instrument) currencyPair, args);
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        return getUserTrades((Instrument) currencyPair, args);
    }


}
