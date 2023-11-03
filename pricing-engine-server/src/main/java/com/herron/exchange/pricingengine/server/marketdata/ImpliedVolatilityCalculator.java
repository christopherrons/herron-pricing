package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.pricingengine.server.marketdata.external.nasdaq.NasdaqYieldCurveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.herron.exchange.common.api.common.enums.Status.ERROR;

public class ImpliedVolatilityCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImpliedVolatilityCalculator.class);
    private final MarketDataService marketDataService;

    public ImpliedVolatilityCalculator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public void createSurfaces(Timestamp timestamp) {
        List<OptionInstrument> options = ReferenceDataCache.getCache().getInstruments().stream()
                .filter(OptionInstrument.class::isInstance)
                .map(OptionInstrument.class::cast)
                .toList();

        Map<Instrument, List<OptionInstrument>> instrumentToOptions = new HashMap<>();
        Map<Instrument, Price> instrumentToPrice = new HashMap<>();
        for (var option : options) {
            var optionPrice = requestPrice(option, timestamp);
            if (optionPrice.status() == ERROR) {
                LOGGER.warn("Removing {} price not found.", option);
                continue;
            }
            var underlying = ReferenceDataCache.getCache().getInstrument(option.underlyingInstrumentId());
            var underlyingPrice = requestPrice(underlying, timestamp);
            if (underlyingPrice.status() == ERROR) {
                LOGGER.error("Removing {} price not found.", underlying);
                continue;
            }

            instrumentToOptions.computeIfAbsent(underlying, k -> new ArrayList<>()).add(option);
            instrumentToPrice.putIfAbsent(underlying, underlyingPrice.marketDataPrice().price());
            instrumentToPrice.putIfAbsent(option, optionPrice.marketDataPrice().price());
        }

        var yieldCurve = requestYieldCurve();
    }

    private MarketDataYieldCurveResponse requestYieldCurve() {
        var request = ImmutableMarketDataYieldCurveRequest.builder()
                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(NasdaqYieldCurveHandler.YIELD_CURVE_ID).build())
                .timeFilter(MarketDataRequestTimeFilter.LATEST)
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.now()).build())
                .build();

        return marketDataService.getYieldCurve(request);
    }

    private MarketDataPriceResponse requestPrice(Instrument instrument, Timestamp timestamp) {
        if (instrument == null) {
            return MarketDataPriceResponse.createErrorResponse("");
        }
        var request = ImmutableMarketDataPriceRequest.builder()
                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(timestamp).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .build();
        return marketDataService.getMarketDataPrice(request);
    }
}
