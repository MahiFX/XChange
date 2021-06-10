package org.knowm.xchange.binance;

import org.knowm.xchange.binance.dto.meta.BinanceTime;

import java.io.IOException;

public interface BinanceCommon {
    /**
     * Test connectivity to the Rest API and get the current server time.
     *
     * @return
     * @throws IOException
     */
    BinanceTime time() throws IOException;
}
