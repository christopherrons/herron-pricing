package com.herron.exchange.pricingengine.server.marketdata.external;

import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataYieldCurve;
import com.herron.exchange.pricingengine.server.marketdata.external.eurex.EurexPreviousDaySettlementHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.nasdaq.NasdaqYieldCurveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class ExternalMarketDataHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalMarketDataHandler.class);
    private final EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler;
    private final NasdaqYieldCurveHandler nasdaqYieldCurveHandler;

    public ExternalMarketDataHandler(EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler, NasdaqYieldCurveHandler nasdaqYieldCurveHandler) {
        this.eurexPreviousDaySettlementHandler = eurexPreviousDaySettlementHandler;
        this.nasdaqYieldCurveHandler = nasdaqYieldCurveHandler;
    }

    public List<MarketDataPrice> getPreviousDaySettlementPrices() {
        return eurexPreviousDaySettlementHandler.getPreviousDaySettlementPrices();
    }

    public List<MarketDataYieldCurve> getYieldCurves(LocalDate from, LocalDate to) {
        return nasdaqYieldCurveHandler.getYieldCurves(from, to);
    }
}
