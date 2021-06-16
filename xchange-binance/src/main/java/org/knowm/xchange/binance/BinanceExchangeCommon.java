package org.knowm.xchange.binance;

import org.knowm.xchange.BaseExchange;
import org.knowm.xchange.binance.dto.account.AssetDetail;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Filter;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Symbol;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceBaseServiceCommon;
import org.knowm.xchange.client.ResilienceRegistries;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.exceptions.ExchangeException;
import si.mazi.rescu.SynchronizedValueFactory;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

public abstract class BinanceExchangeCommon extends BaseExchange {
    protected static ResilienceRegistries RESILIENCE_REGISTRIES;
    protected BinanceExchangeInfo exchangeInfo;
    protected SynchronizedValueFactory<Long> timestampFactory;

    public static void resetResilienceRegistries() {
        RESILIENCE_REGISTRIES = null;
    }

    public SynchronizedValueFactory<Long> getTimestampFactory() {
        return timestampFactory;
    }

    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
        throw new UnsupportedOperationException(
                "Binance uses timestamp/recvwindow rather than a nonce");
    }

    @Override
    public ResilienceRegistries getResilienceRegistries() {
        if (RESILIENCE_REGISTRIES == null) {
            RESILIENCE_REGISTRIES = BinanceResilience.createRegistries();
        }
        return RESILIENCE_REGISTRIES;
    }

    @Override
    public void remoteInit() {

        try {
            // populate currency pair keys only, exchange does not provide any other metadata for download
            Map<CurrencyPair, CurrencyPairMetaData> currencyPairs = exchangeMetaData.getCurrencyPairs();
            Map<Currency, CurrencyMetaData> currencies = exchangeMetaData.getCurrencies();

            BinanceBaseServiceCommon binanceBaseServiceCommon = (BinanceBaseServiceCommon) this.marketDataService;
            exchangeInfo = binanceBaseServiceCommon.getExchangeInfo();
            Symbol[] symbols = exchangeInfo.getSymbols();

            Map<String, AssetDetail> assetDetailMap = null;
            try {
                BinanceAccountService accountService = (BinanceAccountService) getAccountService();
                assetDetailMap = accountService.getAssetDetails();
            } catch (Throwable t) {
                logger.warn("Failed to get asset details from exchange. Using hardcoded ones instead");
            }
            // Clear all hardcoded currencies when loading dynamically from exchange.
            if (assetDetailMap != null) {
                currencies.clear();
            }

            for (Symbol symbol : symbols) {
                if (symbol.getStatus().equals("TRADING")) { // Symbols which are trading
                    int basePrecision = Integer.parseInt(symbol.getBaseAssetPrecision());
                    int counterPrecision = Integer.parseInt(symbol.getQuotePrecision());
                    int pairPrecision = 8;
                    int amountPrecision = 8;

                    BigDecimal minQty = null;
                    BigDecimal maxQty = null;
                    BigDecimal stepSize = null;

                    BigDecimal counterMinQty = null;
                    BigDecimal counterMaxQty = null;

                    Filter[] filters = symbol.getFilters();

                    CurrencyPair currentCurrencyPair =
                            new CurrencyPair(symbol.getBaseAsset(), symbol.getQuoteAsset());

                    for (Filter filter : filters) {
                        if (filter.getFilterType().equals("PRICE_FILTER")) {
                            pairPrecision = Math.min(pairPrecision, numberOfDecimals(filter.getTickSize()));
                            counterMaxQty = new BigDecimal(filter.getMaxPrice()).stripTrailingZeros();
                        } else if (filter.getFilterType().equals("LOT_SIZE")) {
                            amountPrecision = Math.min(amountPrecision, numberOfDecimals(filter.getStepSize()));
                            minQty = new BigDecimal(filter.getMinQty()).stripTrailingZeros();
                            maxQty = new BigDecimal(filter.getMaxQty()).stripTrailingZeros();
                            stepSize = new BigDecimal(filter.getStepSize()).stripTrailingZeros();
                        } else if (filter.getFilterType().equals("MIN_NOTIONAL")) {
                            if (filter.getMinNotional() != null)
                                counterMinQty = new BigDecimal(filter.getMinNotional()).stripTrailingZeros();
                        }
                    }

                    boolean marketOrderAllowed = Arrays.asList(symbol.getOrderTypes()).contains("MARKET");
                    currencyPairs.put(
                            currentCurrencyPair,
                            new CurrencyPairMetaData(
                                    new BigDecimal("0.1"), // Trading fee at Binance is 0.1 %
                                    minQty, // Min amount
                                    maxQty, // Max amount
                                    counterMinQty,
                                    counterMaxQty,
                                    amountPrecision, // base precision
                                    pairPrecision, // counter precision
                                    null,
                                    null, /* TODO get fee tiers, although this is not necessary now
                          because their API returns current fee directly */
                                    stepSize,
                                    null,
                                    marketOrderAllowed));

                    Currency baseCurrency = currentCurrencyPair.base;
                    CurrencyMetaData baseCurrencyMetaData =
                            BinanceAdapters.adaptCurrencyMetaData(
                                    currencies, baseCurrency, assetDetailMap, basePrecision);
                    currencies.put(baseCurrency, baseCurrencyMetaData);

                    Currency counterCurrency = currentCurrencyPair.counter;
                    CurrencyMetaData counterCurrencyMetaData =
                            BinanceAdapters.adaptCurrencyMetaData(
                                    currencies, counterCurrency, assetDetailMap, counterPrecision);
                    currencies.put(counterCurrency, counterCurrencyMetaData);
                }
            }
        } catch (Exception e) {
            throw new ExchangeException("Failed to initialize: " + e.getMessage(), e);
        }
    }

    private int numberOfDecimals(String value) {
        return new BigDecimal(value).stripTrailingZeros().scale();
    }

    public BinanceExchangeInfo getExchangeInfo() {
        return exchangeInfo;
    }
}
