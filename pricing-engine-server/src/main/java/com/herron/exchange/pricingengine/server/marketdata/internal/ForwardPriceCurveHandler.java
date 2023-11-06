package com.herron.exchange.pricingengine.server.marketdata.internal;

import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataForwardPriceCurve;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.common.api.common.parametricmodels.yieldcurve.YieldCurve;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.marketdata.external.nasdaq.NasdaqYieldCurveHandler;
import com.herron.exchange.quantlib.parametricmodels.ForwardPriceCurveConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.herron.exchange.common.api.common.enums.Status.ERROR;

public class ForwardPriceCurveHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardPriceCurveHandler.class);
    private final MarketDataService marketDataService;

    public ForwardPriceCurveHandler(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public List<MarketDataForwardPriceCurve> createForwardPriceCurves(Timestamp valuationTime) {
        LOGGER.info("Creating forward curve for {}.", valuationTime);
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
        return constructCurves(valuationTime, underlyingInstrumentToOptions, instrumentToPrice, yieldCurveResponse.yieldCurveEntry().yieldCurve());
    }

    private List<MarketDataForwardPriceCurve> constructCurves(Timestamp valuationTime,
                                                              Map<Instrument, List<OptionInstrument>> underlyingInstrumentToOptions,
                                                              Map<Instrument, Price> instrumentToPrice,
                                                              YieldCurve yieldCurve) {
        List<MarketDataForwardPriceCurve> curves = new ArrayList<>();
        for (var entry : underlyingInstrumentToOptions.entrySet()) {
            var underlying = entry.getKey();
            var options = entry.getValue();
            var curve = ForwardPriceCurveConstructor.construct(valuationTime, underlying, options, instrumentToPrice, yieldCurve);
            curves.add(MarketDataForwardPriceCurve.create(valuationTime, underlying.instrumentId(), curve));
        }
        return curves;
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
