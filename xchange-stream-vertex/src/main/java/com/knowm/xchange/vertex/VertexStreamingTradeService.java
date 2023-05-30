package com.knowm.xchange.vertex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowm.xchange.vertex.dto.*;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.SignatureAndDigest;
import com.knowm.xchange.vertex.signing.schemas.CancelOrdersSchema;
import com.knowm.xchange.vertex.signing.schemas.PlaceOrderSchema;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.disposables.Disposable;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.knowm.xchange.service.trade.params.CancelOrderByIdParams;
import org.knowm.xchange.service.trade.params.CancelOrderByInstrument;
import org.knowm.xchange.service.trade.params.CancelOrderParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.buildNonce;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.convertToInteger;

public class VertexStreamingTradeService implements StreamingTradeService, TradeService {

    public static final double MAX_SLIPPAGE_RATIO = 0.005;
    private final Logger logger = LoggerFactory.getLogger(VertexStreamingTradeService.class);

    private final VertexStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;
    private final ObjectMapper mapper;

    private final VertexProductInfo productInfo;
    private final long chainId;
    private final String bookContract;
    private final VertexStreamingExchange exchange;
    private final String endpointContract;

    public VertexStreamingTradeService(VertexStreamingService streamingService, ExchangeSpecification exchangeSpecification, VertexProductInfo productInfo, long chainId, String bookContract, VertexStreamingExchange exchange, String endpointContract) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
        this.productInfo = productInfo;
        this.chainId = chainId;
        this.bookContract = bookContract;
        this.endpointContract = endpointContract;
        this.exchange = exchange;
        this.mapper = StreamingObjectMapperHelper.getObjectMapper();
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {

        BigDecimal price = limitOrder.getLimitPrice();

        return placeOrder(limitOrder, price);
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {

        long productId = productInfo.lookupProductId(marketOrder.getCurrencyPair());

        BigDecimal price = getPrice(marketOrder, productId);

        return placeOrder(marketOrder, price);
    }

    private String placeOrder(Order marketOrder, BigDecimal price) throws JsonProcessingException {


        BigInteger expiration = getExpiration(marketOrder.getOrderFlags(), Instant.now().plus(10, ChronoUnit.SECONDS));

        long productId = productInfo.lookupProductId(marketOrder.getCurrencyPair());

        Pair<BigDecimal, BigDecimal> increments = exchange.getIncrements(productId);
        BigDecimal priceIncrement = increments.getRight();
        price = roundToIncrement(price, priceIncrement);

        BigDecimal quantity = getQuantity(marketOrder);
        BigDecimal quantityIncrement = increments.getLeft();
        if (quantity.abs().compareTo(quantityIncrement) < 0) {
            throw new IllegalArgumentException("Quantity must be greater than increment");
        }
        quantity = roundToIncrement(quantity, quantityIncrement);

        BigInteger quantityAsInt = convertToInteger(quantity);
        BigInteger priceAsInt = convertToInteger(price);

        String subAccount = exchangeSpecification.getUserName();
        String nonce = buildNonce(60000);
        String walletAddress = exchangeSpecification.getApiKey();
        String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

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
                signatureAndDigest.getSignature()
        ));


        sendWebsocketMessage(orderMessage);

        return signatureAndDigest.getDigest();
    }

    private boolean sendWebsocketMessage(Object messageObj) throws JsonProcessingException {

        streamingService.connect().blockingAwait();
        CountDownLatch readyLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorHolder = new AtomicReference<>();
        Disposable respSub = streamingService.subscribeChannel(ALL_MESSAGES).subscribe(resp -> {
            if (resp.get("status").asText().equals("success")) {
                logger.info("Received response: {}", resp);
            } else {
                logger.error("Received error: {}", resp);
                errorHolder.set(new RuntimeException("Websocket message error: " + resp.get("error").asText()));
            }
            readyLatch.countDown();
        });
        String message = mapper.writeValueAsString(messageObj);
        logger.info("Sending order: {}", message);
        try {
            streamingService.sendMessage(message);

            if (!readyLatch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timed out waiting for response");
            }
            if (errorHolder.get() != null) {
                return false;
            } else {
                return true;
            }
        } catch (InterruptedException ignored) {
        } finally {
            respSub.dispose();
        }
        return false;
    }

    private BigInteger getExpiration(Set<Order.IOrderFlags> orderFlags, Instant expiryTime) {
        BigInteger timeInForce = BigInteger.ZERO; // resting
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
            Pair<BigDecimal, BigDecimal> bidOffer = exchange.getMarketPrice(productId);
            boolean isSell = order.getType().equals(Order.OrderType.ASK);
            if (isSell) {
                BigDecimal bid = bidOffer.getLeft();
                // subtract max slippage from bid
                price = bid.subtract(bid.multiply(BigDecimal.valueOf(MAX_SLIPPAGE_RATIO)));
            } else {
                BigDecimal offer = bidOffer.getLeft();
                // add max slippage to offer
                price = offer.add(offer.multiply(BigDecimal.valueOf(MAX_SLIPPAGE_RATIO)));
            }
        }
        return price;
    }

    @Override
    public boolean cancelOrder(CancelOrderParams params) throws IOException {

        if (params instanceof CancelOrderByIdParams && params instanceof CancelOrderByInstrument) {

            String id = ((CancelOrderByIdParams) params).getOrderId();
            CurrencyPair instrument = (CurrencyPair) ((CancelOrderByInstrument) params).getInstrument();

            long productId = productInfo.lookupProductId(instrument);

            String subAccount = exchangeSpecification.getUserName();
            String nonce = buildNonce(60000);
            String walletAddress = exchangeSpecification.getApiKey();
            String sender = VertexModelUtils.buildSender(walletAddress, subAccount);
            long[] productIds = {productId};
            String[] digests = {id};

            CancelOrdersSchema orderSchema = CancelOrdersSchema.build(chainId,
                    endpointContract, Long.valueOf(nonce), sender, productIds, digests);
            SignatureAndDigest signatureAndDigest = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

            VertexCancelOrdersMessage orderMessage = new VertexCancelOrdersMessage(new CancelOrders(
                    new Tx(sender, productIds, digests, nonce),
                    signatureAndDigest.getSignature()
            ));


            return sendWebsocketMessage(orderMessage);

        } else {
            throw new IOException(
                    "CancelOrderParams must implement CancelOrderByIdParams and CancelOrderByInstrument interface.");
        }


    }

    private BigDecimal getQuantity(Order order) {
        BigDecimal quantityAsInt = order.getOriginalAmount();
        if (order.getType().equals(Order.OrderType.ASK)) {
            quantityAsInt = quantityAsInt.multiply(BigDecimal.valueOf(-1));
        }
        return quantityAsInt;
    }

    public static BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
        if (increment.equals(0)) return value;
        BigDecimal divided = value.divide(increment, 0, RoundingMode.FLOOR);
        return divided.multiply(increment);
    }
}
