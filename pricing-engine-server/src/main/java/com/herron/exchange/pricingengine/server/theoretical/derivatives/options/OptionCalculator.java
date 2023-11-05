package com.herron.exchange.pricingengine.server.theoretical.derivatives.options;

import com.herron.exchange.common.api.common.api.pricing.PriceModelResult;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.OptionInstrument;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.enums.Status;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataForwardPriceCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataImpliedVolatilitySurfaceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataYieldCurveRequest;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataForwardPriceCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataImpliedVolatilitySurfaceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataPriceResponse;
import com.herron.exchange.common.api.common.messages.marketdata.response.MarketDataYieldCurveResponse;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataForwardPriceCurveStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataImpliedVolatilitySurfaceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataYieldCurveStaticKey;
import com.herron.exchange.common.api.common.messages.pricing.BlackScholesPriceModelParameters;
import com.herron.exchange.common.api.common.messages.pricing.FailedPriceModelResult;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.quantlib.pricemodels.derivatives.options.Black76;
import com.herron.exchange.quantlib.pricemodels.derivatives.options.BlackScholesMerton;

public class OptionCalculator {
    private final MarketDataService marketDataService;

    public OptionCalculator(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public PriceModelResult calculate(OptionInstrument option, Timestamp valuationTime) {
        return switch (option.priceModel()) {
            case BLACK_SCHOLES -> calculateWithBlackScholes(option, valuationTime);
            case BLACK_76 -> calculateWithBlack76(option, valuationTime);
            case BARONE_ADESI_WHALEY -> FailedPriceModelResult.createFailedResult("");
            default -> FailedPriceModelResult.createFailedResult(String.format("Option price model %s not supported.", option));
        };
    }

    private PriceModelResult calculateWithBlackScholes(OptionInstrument option, Timestamp valuationTime) {
        var parameters = (BlackScholesPriceModelParameters) option.priceModelParameters();
        var yieldCurveResponse = requestYieldCurve(parameters.yieldCurveId(), valuationTime);
        if (yieldCurveResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(yieldCurveResponse.error());
        }

        var underlyingPriceResponse = requestPrice(option, valuationTime);
        if (underlyingPriceResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(underlyingPriceResponse.error());
        }

        var impliedVolatilitySurfaceResponse = requestVolatilitySurface(option.instrumentId(), valuationTime);
        if (impliedVolatilitySurfaceResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(impliedVolatilitySurfaceResponse.error());
        }

        var ivSurface = impliedVolatilitySurfaceResponse.impliedVolatilitySurfaceEntry().impliedVolatilitySurface();

        double ttm = BlackScholesMerton.calculateTimeToMaturity(valuationTime, option);
        double strikePrice = option.strikePrice().getRealValue();
        double spotPrice = underlyingPriceResponse.marketDataPrice().price().getRealValue();
        double riskFreeRate = yieldCurveResponse.yieldCurveEntry().yieldCurve().getYield(ttm);
        double logMoneyness = Math.log(strikePrice / spotPrice);
        double impliedVolatility = ivSurface.getImpliedVolatility(ttm, logMoneyness, option.optionType());
        return BlackScholesMerton.calculateOptionPrice(
                valuationTime,
                option.optionType(),
                strikePrice,
                spotPrice,
                impliedVolatility,
                ttm,
                riskFreeRate,
                parameters.dividendYield().getRealValue()
        );
    }

    private PriceModelResult calculateWithBlack76(OptionInstrument option, Timestamp valuationTime) {
        var parameters = (BlackScholesPriceModelParameters) option.priceModelParameters();
        var yieldCurveResponse = requestYieldCurve(parameters.yieldCurveId(), valuationTime);
        if (yieldCurveResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(yieldCurveResponse.error());
        }

        var underlyingPriceResponse = requestPrice(option, valuationTime);
        if (underlyingPriceResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(underlyingPriceResponse.error());
        }

        var impliedVolatilitySurfaceResponse = requestVolatilitySurface(option.underlyingInstrumentId(), valuationTime);
        if (impliedVolatilitySurfaceResponse.status() == Status.ERROR) {
            return FailedPriceModelResult.createFailedResult(impliedVolatilitySurfaceResponse.error());
        }
        var ivSurface = impliedVolatilitySurfaceResponse.impliedVolatilitySurfaceEntry().impliedVolatilitySurface();

        var forwardPriceCurveResponse = requestForwardPriceCurve(option.underlyingInstrumentId(), valuationTime);

        double ttm = Black76.calculateTimeToMaturity(valuationTime, option);
        double strikePrice = option.strikePrice().getRealValue();
        double spotPrice = underlyingPriceResponse.marketDataPrice().price().getRealValue();
        double riskFreeRate = yieldCurveResponse.yieldCurveEntry().yieldCurve().getYield(ttm);
        double logMoneyness = Math.log(strikePrice / spotPrice);
        double impliedVolatility = ivSurface.getImpliedVolatility(ttm, logMoneyness, option.optionType());
        double dividendYield = parameters.dividendYield().getRealValue();
        double forwardPrice = spotPrice * Math.exp((riskFreeRate - dividendYield) * ttm);
        if (forwardPriceCurveResponse.status() == Status.OK) {
            forwardPrice = forwardPriceCurveResponse.forwardPriceCurveEntry().forwardPriceCurve().getForwardPrice(ttm);
        }

        return Black76.calculateOptionPrice(
                valuationTime,
                option.optionType(),
                strikePrice,
                forwardPrice,
                impliedVolatility,
                ttm,
                riskFreeRate
        );
    }

    private MarketDataForwardPriceCurveResponse requestForwardPriceCurve(String underlyingInstrumentId, Timestamp valuationTime) {
        var request = ImmutableMarketDataForwardPriceCurveRequest.builder()
                .staticKey(ImmutableMarketDataForwardPriceCurveStaticKey.builder().instrumentId(underlyingInstrumentId).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .build();

        return marketDataService.getForwardPriceCurve(request);
    }

    private MarketDataImpliedVolatilitySurfaceResponse requestVolatilitySurface(String underlyingInstrumentId, Timestamp valuationTime) {
        var request = ImmutableMarketDataImpliedVolatilitySurfaceRequest.builder()
                .staticKey(ImmutableMarketDataImpliedVolatilitySurfaceStaticKey.builder().instrumentId(underlyingInstrumentId).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                .build();

        return marketDataService.getImpliedVolatilitySurface(request);
    }

    private MarketDataYieldCurveResponse requestYieldCurve(String curveId, Timestamp valuationTime) {
        var request = ImmutableMarketDataYieldCurveRequest.builder()
                .staticKey(ImmutableMarketDataYieldCurveStaticKey.builder().curveId(curveId).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(valuationTime).build())
                .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
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
