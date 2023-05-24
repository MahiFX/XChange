package com.knowm.xchange.vertex;

import info.bitrich.xchangestream.core.StreamingTradeService;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.service.trade.TradeService;

public class VertexStreamingTradeService implements StreamingTradeService, TradeService {
    public VertexStreamingTradeService(VertexStreamingService streamingService, ExchangeSpecification exchangeSpecification) {
    }
}
