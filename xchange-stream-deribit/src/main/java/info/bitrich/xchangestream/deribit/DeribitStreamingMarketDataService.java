package info.bitrich.xchangestream.deribit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.deribit.dto.DeribitMarketDataUpdateMessage;
import info.bitrich.xchangestream.deribit.dto.DeribitWebsocketResponse;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotAvailableFromExchangeException;

public class DeribitStreamingMarketDataService implements StreamingMarketDataService {
    private final DeribitStreamingService streamingService;

    private final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    public DeribitStreamingMarketDataService(DeribitStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        String channelName = "book." + currencyPair.toString().replace("/", "-") + ".raw";

        DeribitOrderBook orderBook = new DeribitOrderBook(currencyPair);

        // TODO: Handle reset on sequence number skip
        Disposable orderBookSubscriptionDisposable = streamingService.subscribeChannel(channelName)
                .map(json -> {
                    if (json.has("params")) {
                        JsonNode params = json.get("params");
                        if (params.has("data")) {
                            JsonNode data = params.get("data");

                            return mapper.treeToValue(data, DeribitMarketDataUpdateMessage.class);
                        }
                    }

                    return DeribitMarketDataUpdateMessage.EMPTY;
                })
                .filter(update -> update != DeribitMarketDataUpdateMessage.EMPTY)
                .subscribe(orderBook, t -> {
                    throw new RuntimeException(t);
                });

        return orderBook;
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        throw new NotAvailableFromExchangeException();
    }
}
