package org.knowm.xchange.binance.service;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.BinanceFuturesExchange;
import org.knowm.xchange.binance.dto.*;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.knowm.xchange.binance.BinanceResilience.*;
import static org.knowm.xchange.client.ResilienceRegistries.NON_IDEMPOTENT_CALLS_RETRY_CONFIG_NAME;

public class BinanceFuturesTradeServiceRaw extends BinanceFuturesBaseService {
    public BinanceFuturesTradeServiceRaw(
            BinanceFuturesExchange exchange,
            BinanceFuturesAuthenticated binanceFutures,
            ResilienceRegistries resilienceRegistries) {
        super(exchange, binanceFutures, resilienceRegistries);
    }

    public BinanceFuturesOrder newOrder(
            CurrencyPair pair,
            OrderSide side,
            PositionSide positionSide,
            OrderType type,
            TimeInForce timeInForce,
            BigDecimal quantity,
            Boolean reduceOnly,
            BigDecimal price,
            String newClientOrderId,
            BigDecimal stopPrice,
            Boolean closePosition,
            BigDecimal activationPrice,
            BigDecimal callbackRate,
            WorkingType workingType,
            Boolean priceProtect)
            throws IOException, BinanceException {
        return decorateApiCall(
                () ->
                        binanceFutures.newOrder(
                                BinanceAdapters.toSymbol(pair),
                                side,
                                positionSide,
                                type,
                                timeInForce,
                                quantity,
                                reduceOnly,
                                price,
                                newClientOrderId,
                                stopPrice,
                                closePosition,
                                activationPrice,
                                callbackRate,
                                workingType,
                                priceProtect,
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator))
                .withRetry(retry("newOrder", NON_IDEMPOTENT_CALLS_RETRY_CONFIG_NAME))
                .withRateLimiter(rateLimiter(ORDERS_PER_SECOND_RATE_LIMITER))
                .withRateLimiter(rateLimiter(ORDERS_PER_DAY_RATE_LIMITER))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }

    public BinanceFuturesOrder getOrderStatus(
            CurrencyPair currencyPair,
            Long orderId,
            String clientOrderId)
            throws IOException, BinanceException {
        return decorateApiCall(
                () ->
                        binanceFutures.getOrder(
                                BinanceAdapters.toSymbol(currencyPair),
                                orderId,
                                clientOrderId,
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator
                        ))
                .withRetry(retry("orderStatus"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }

    public BinanceFuturesOrder cancelOrder(
            CurrencyPair currencyPair,
            Long orderId,
            String clientOrderId)
            throws IOException, BinanceException {
        return decorateApiCall(
                () ->
                        binanceFutures.cancelOrder(
                                BinanceAdapters.toSymbol(currencyPair),
                                orderId,
                                clientOrderId,
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator
                        ))
                .withRetry(retry("cancelOrder"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }

    public void cancelAllOpenOrders(CurrencyPair currencyPair)
            throws IOException, BinanceException {
        decorateApiCall(
                () ->
                        binanceFutures.cancelAllOpenOrders(
                                BinanceAdapters.toSymbol(currencyPair),
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator
                        ))
                .withRetry(retry("cancelAllOpenOrders"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }

    public List<BinanceFuturesOrder> getAllOpenOrders() throws IOException, BinanceException {
        return getAllOpenOrders(null);
    }

    public List<BinanceFuturesOrder> getAllOpenOrders(CurrencyPair currencyPair) throws IOException, BinanceException {
        return decorateApiCall(
                () ->
                        binanceFutures.getAllOpenOrders(
                                currencyPair != null ? BinanceAdapters.toSymbol(currencyPair) : null,
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator
                        ))
                .withRetry(retry("openOrders"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }

    public List<BinancePosition> getAllOpenPositions() throws IOException, BinanceException {
        return decorateApiCall(
                () ->
                        binanceFutures.getOpenPositions(
                                null,
                                getRecvWindow(),
                                getTimestampFactory(),
                                apiKey,
                                signatureCreator
                        ))
                .withRetry(retry("openPositions"))
                .withRateLimiter(rateLimiter(REQUEST_WEIGHT_RATE_LIMITER))
                .call();
    }
}
