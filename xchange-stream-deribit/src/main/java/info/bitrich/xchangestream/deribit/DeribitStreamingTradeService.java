package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.deribit.dto.DeribitOrderUpdate;
import info.bitrich.xchangestream.deribit.dto.DeribitUserTrade;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DeribitStreamingTradeService implements StreamingTradeService, TradeService {
    private static final Logger logger = LoggerFactory.getLogger(DeribitStreamingTradeService.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final DeribitStreamingService streamingService;
    private final ExchangeSpecification exchangeSpecification;

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingTradeService(DeribitStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
        this.streamingService = streamingService;
        this.exchangeSpecification = exchangeSpecification;
    }

    @Override
    public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
        authenticate();

        String channelName = "user.orders." + DeribitStreamingUtil.instrumentName(currencyPair) + ".raw";

        logger.info("Subscribing to: " + channelName);

        return streamingService.subscribeChannel(channelName)
                .map(json -> {
                    DeribitOrderUpdate deribitOrderUpdate = DeribitStreamingUtil.tryGetDataAsType(mapper, json, DeribitOrderUpdate.class);

                    if (deribitOrderUpdate == null) {
                        return DeribitOrderUpdate.EMPTY;
                    } else {
                        return deribitOrderUpdate;
                    }
                })
                .filter(update -> update != DeribitOrderUpdate.EMPTY)
                .map(DeribitOrderUpdate::toOrder);
    }

    @Override
    public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
        authenticate();

        String channelName = "user.trades." + DeribitStreamingUtil.instrumentName(currencyPair) + ".raw";

        logger.info("Subscribing to: " + channelName);

        return streamingService.subscribeChannel(channelName)
                .map(json -> {
                    DeribitUserTrade deribitUserTrade = DeribitStreamingUtil.tryGetDataAsType(mapper, json, DeribitUserTrade.class);

                    if (deribitUserTrade == null) {
                        return DeribitUserTrade.EMPTY;
                    } else {
                        return deribitUserTrade;
                    }
                })
                .filter(update -> update != DeribitUserTrade.EMPTY)
                .map(DeribitUserTrade::toUserTrade);
    }

    @Override
    public String placeLimitOrder(LimitOrder limitOrder) throws IOException {
        throw new NotYetImplementedForExchangeException();
    }

    @Override
    public String placeMarketOrder(MarketOrder marketOrder) throws IOException {
        throw new NotYetImplementedForExchangeException();
    }

    private void authenticate() {
        try {
            streamingService.authenticate(exchangeSpecification.getApiKey(), exchangeSpecification.getSecretKey());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
