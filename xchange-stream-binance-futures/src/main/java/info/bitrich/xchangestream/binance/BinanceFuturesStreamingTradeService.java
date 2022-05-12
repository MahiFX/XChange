package info.bitrich.xchangestream.binance;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.binance.dto.BinanceFuturesOrderUpdate;
import info.bitrich.xchangestream.binance.dto.ExecutionReportBinanceUserTransaction;
import org.knowm.xchange.exceptions.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BinanceFuturesStreamingTradeService extends BinanceStreamingTradeService {
    public BinanceFuturesStreamingTradeService(BinanceUserDataStreamingService binanceUserDataStreamingService) {
        super(binanceUserDataStreamingService);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void openSubscriptions() {
        if (binanceUserDataStreamingService != null) {
            executionReports = binanceUserDataStreamingService
                    .subscribeChannel("ORDER_TRADE_UPDATE")
                    .map(this::orderUpdateToExecutionReport)
                    .subscribe(
                            executionReportsPublisher::onNext,
                            executionReportsPublisher::onError
                    );
        }
    }

    private ExecutionReportBinanceUserTransaction orderUpdateToExecutionReport(JsonNode json) {
        try {
            BinanceFuturesOrderUpdate binanceFuturesOrderUpdate = mapper.treeToValue(json, BinanceFuturesOrderUpdate.class);

            return binanceFuturesOrderUpdate.toBinanceExecutionReport();
        } catch (IOException e) {
            throw new ExchangeException("Unable to parse execution report", e);
        }
    }
}
