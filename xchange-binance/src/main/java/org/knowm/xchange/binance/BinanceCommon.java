package org.knowm.xchange.binance;

import org.knowm.xchange.binance.dto.BinanceException;
import org.knowm.xchange.binance.dto.marketdata.BinanceOrderbook;
import org.knowm.xchange.binance.dto.meta.BinanceTime;

import javax.ws.rs.QueryParam;
import java.io.IOException;

public interface BinanceCommon {
    /**
     * Test connectivity to the Rest API and get the current server time.
     *
     * @return
     * @throws IOException
     */
    BinanceTime time() throws IOException;

    /**
     * @param symbol
     * @param limit  optional, default 100 max 5000. Valid limits: [5, 10, 20, 50, 100, 500, 1000,
     *               5000]
     * @return
     * @throws IOException
     * @throws BinanceException
     */
    BinanceOrderbook depth(@QueryParam("symbol") String symbol, @QueryParam("limit") Integer limit)
            throws IOException, BinanceException;
}
