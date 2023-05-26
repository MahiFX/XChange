package com.knowm.xchange.vertex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowm.xchange.vertex.dto.VertexModelUtils;
import com.knowm.xchange.vertex.dto.VertexOrder;
import com.knowm.xchange.vertex.dto.VertexOrderMessage;
import com.knowm.xchange.vertex.dto.VertexPlaceOrder;
import com.knowm.xchange.vertex.signing.MessageSigner;
import com.knowm.xchange.vertex.signing.schemas.PlaceOrderSchema;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static com.knowm.xchange.vertex.VertexStreamingService.ALL_MESSAGES;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.buildNonce;
import static com.knowm.xchange.vertex.dto.VertexModelUtils.convertToInteger;

public class VertexStreamingTradeService implements StreamingTradeService, TradeService {

    private final Logger logger = LoggerFactory.getLogger(VertexStreamingTradeService.class);

    private final VertexStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;
    private final ObjectMapper mapper;

    private final VertexProductInfo productInfo;
    private final String chainId;
    private final String bookContract;

    private final Map<Long, Map<String, String>> orderNonceToIdMap = new ConcurrentHashMap<>();

    public VertexStreamingTradeService(VertexStreamingService streamingService, ExchangeSpecification exchangeSpecification, VertexProductInfo productInfo, String chainId, String bookContract) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
        this.productInfo = productInfo;
        this.chainId = chainId;
        this.bookContract = bookContract;
        this.mapper = StreamingObjectMapperHelper.getObjectMapper();
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {

        String subAccount = exchangeSpecification.getUserName();

        String nonce = buildNonce(60000);
        String walletAddress = exchangeSpecification.getApiKey();

        String sender = VertexModelUtils.buildSender(walletAddress, subAccount);

        PlaceOrderSchema orderSchema = PlaceOrderSchema.build(marketOrder, chainId, bookContract, Long.valueOf(nonce), sender);
        String signature = new MessageSigner(exchangeSpecification.getSecretKey()).signMessage(orderSchema);

        long productId = productInfo.lookupProductId(marketOrder.getCurrencyPair());

        BigDecimal maxPrice = BigDecimal.valueOf(50_000);   //FIXME market price + slippage

        BigDecimal quantity = marketOrder.getType() == org.knowm.xchange.dto.Order.OrderType.BID ? marketOrder.getOriginalAmount() : marketOrder.getOriginalAmount().multiply(BigDecimal.valueOf(-1));

        VertexOrderMessage orderMessage = new VertexOrderMessage(new VertexPlaceOrder(
                productId,
                new VertexOrder(
                        sender,
                        convertToInteger(maxPrice).toString(),
                        convertToInteger(quantity).toString(),
                        getExpiration(marketOrder.getOrderFlags()),
                        nonce
                ),
                signature
        ));
        orderNonceToIdMap.computeIfAbsent(productId, k -> new ConcurrentHashMap<>()).put(nonce, marketOrder.getUserReference());

        streamingService.connect().blockingAwait();
        CountDownLatch readyLatch = new CountDownLatch(1);
        streamingService.subscribeChannel(ALL_MESSAGES).subscribe(resp -> {
            if (resp.get("status").asText().equals("success")) {
                logger.info("Received order response: {}", resp);
            } else {
                logger.error("Received order error: {}", resp);
            }
            readyLatch.countDown();
        });
        String message = mapper.writeValueAsString(orderMessage);
        logger.info("Sending order: {}", message);
        streamingService.sendMessage(message);
        try {
            if (!readyLatch.await(2000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timed out waiting for response");
            }
        } catch (InterruptedException ignored) {
        }

        //FIXME return order id

        return nonce;
    }

    private String getExpiration(Set<org.knowm.xchange.dto.Order.IOrderFlags> orderFlags) {
        //FIXME TIF support
        return String.valueOf(Instant.now().plusMillis(60000).toEpochMilli());
    }
}
