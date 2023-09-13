package org.knowm.xchange.binance.futures.service;

import org.knowm.xchange.binance.BinanceAdapters;
import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.trade.OrderSide;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.futures.BinanceFuturesAuthenticated;
import org.knowm.xchange.binance.futures.BinanceFuturesExchange;
import org.knowm.xchange.binance.futures.dto.*;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.instrument.Instrument;
import si.mazi.rescu.SynchronizedValueFactory;

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
            Instrument pair,
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
                        binanceFutures.cancelAllFuturesOpenOrders(
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

    public Long getRecvWindow() {
        Object obj =
                exchange.getExchangeSpecification().getExchangeSpecificParametersItem("recvWindow");
        if (obj == null) return null;
        if (obj instanceof Number) {
            long value = ((Number) obj).longValue();
            if (value < 0 || value > 60000) {
                throw new IllegalArgumentException(
                        "Exchange-specific parameter \"recvWindow\" must be in the range [0, 60000].");
            }
            return value;
        }
        if (obj.getClass().equals(String.class)) {
            try {
                return Long.parseLong((String) obj, 10);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Exchange-specific parameter \"recvWindow\" could not be parsed.", e);
            }
        }
        throw new IllegalArgumentException(
                "Exchange-specific parameter \"recvWindow\" could not be parsed.");
    }

    public SynchronizedValueFactory<Long> getTimestampFactory() {
        return exchange.getTimestampFactory();
    }
}
