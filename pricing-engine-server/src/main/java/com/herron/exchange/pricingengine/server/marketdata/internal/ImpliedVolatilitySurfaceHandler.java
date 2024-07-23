package com.herron.exchange.pricingengine.server.marketdata.internal;

import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.math.parametricmodels.yieldcurve.YieldCurve;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataImpliedVolatilitySurface;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataForwardPriceCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataForwardPriceCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataForwardPriceCurveStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.marketdata.external.nasdaq.NasdaqYieldCurveHandler;
import com.herron.exchange.quantlib.parametricmodels.ivsurface.ImpliedVolatilityConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.herron.exchange.common.api.common.enums.Status.ERROR;
import static com.herron.exchange.common.api.common.enums.Status.OK;

public class ImpliedVolatilitySurfaceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImpliedVolatilitySurfaceHandler.class);
    private final MarketDataService marketDataService;

    public ImpliedVolatilitySurfaceHandler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public List<MarketDataImpliedVolatilitySurface> createSurfaces(Timestamp valuationTime) {
        LOGGER.info("Creating implied volatility surface for {}.", valuationTime);

        List<OptionInstrument> options = ReferenceDataCache.getCache().getInstruments().stream()
                .filter(OptionInstrument.class::isInstance)
                .map(OptionInstrument.class::cast)
                .toList();

        Map<Instrument, List<OptionInstrument>> underlyingInstrumentToOptions = new HashMap<>();
        Map<Instrument, Price> instrumentToPrice = new HashMap<>();
        for (var option : options) {
            var optionPrice = requestPrice(option, valuationTime);
            if (optionPrice.status() == ERROR) {
                LOGGER.warn("Removing {} price not found.", option);
                continue;
            }

            var underlying = ReferenceDataCache.getCache().getInstrument(option.underlyingInstrumentId());
            if (!instrumentToPrice.containsKey(underlying)) {
                var underlyingPrice = requestPrice(underlying, valuationTime);
                if (underlyingPrice.status() == ERROR) {
                    LOGGER.error("Removing {} price not found.", underlying);
                    continue;
                }
                instrumentToPrice.putIfAbsent(underlying, underlyingPrice.marketDataPrice().price());
            }

            underlyingInstrumentToOptions.computeIfAbsent(underlying, k -> new ArrayList<>()).add(option);
            instrumentToPrice.putIfAbsent(option, optionPrice.marketDataPrice().price());
        }

        var yieldCurveResponse = requestYieldCurve(valuationTime);
        if (yieldCurveResponse.status() == ERROR) {
            LOGGER.error("Yield curve not found.", yieldCurveResponse);
            return List.of();
        }
        return constructSurfaces(valuationTime, underlyingInstrumentToOptions, instrumentToPrice, yieldCurveResponse.yieldCurveEntry().yieldCurve());
    }

    private List<MarketDataImpliedVolatilitySurface> constructSurfaces(Timestamp valuationTime,
                                                                       Map<Instrument, List<OptionInstrument>> underlyingInstrumentToOptions,
                                                                       Map<Instrument, Price> instrumentToPrice,
                                                                       YieldCurve yieldCurve) {
        List<MarketDataImpliedVolatilitySurface> ivSurfaces = new ArrayList<>();
        for (var entry : underlyingInstrumentToOptions.entrySet()) {
            var underlying = entry.getKey();
            var forwardCurveResponse = requestForwardPriceCurve(underlying.instrumentId(), valuationTime);
            var forwardCurve = forwardCurveResponse.status() == OK ? forwardCurveResponse.forwardPriceCurveEntry().forwardPriceCurve() : null;
            var options = entry.getValue();
            var surface = ImpliedVolatilityConstructor.construct(valuationTime, underlying, options, instrumentToPrice, yieldCurve, forwardCurve);
            ivSurfaces.add(MarketDataImpliedVolatilitySurface.create(valuationTime, underlying.instrumentId(), surface));
        }
        return ivSurfaces;
    }

    private MarketDataForwardPriceCurveResponse requestForwardPriceCurve(String underlyingInstrumentId, Timestamp valuationTime) {
        var request = ImmutableMarketDataForwardPriceCurveRequest.builder()
                .staticKey(ImmutableMarketDataForwardPriceCurveStaticKey.builder().instrumentId(underlyingInstrumentId).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .build();

        return marketDataService.getForwardPriceCurve(request);
    }

    private MarketDataYieldCurveResponse requestYieldCurve(Timestamp valuationTime) {
        var request = ImmutableMarketDataYieldCurveRequest.builder()
                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(NasdaqYieldCurveHandler.YIELD_CURVE_ID).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .build();

        return marketDataService.getYieldCurve(request);
    }

    private MarketDataPriceResponse requestPrice(Instrument instrument, Timestamp valuationTime) {
        if (instrument == null) {
            return MarketDataPriceResponse.createErrorResponse("");
        }
        var request = ImmutableMarketDataPriceRequest.builder()
                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .build();
        return marketDataService.getMarketDataPrice(request);
    }
}
