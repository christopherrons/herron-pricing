package com.herron.exchange.pricingengine.server.snapshot;


import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.enums.Status;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.trading.PriceQuote;
import com.herron.exchange.common.api.common.messages.trading.Trade;
import com.herron.exchange.pricingengine.server.pricemodels.TheoreticalPriceCalculator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.herron.exchange.common.api.common.enums.PriceType.*;

public class PriceSnapshotCalculator {

    private final VwapCalculator vwapCalculator = new VwapCalculator();
    private final SnapshotPriceThrottleFilter throttleFilter = new SnapshotPriceThrottleFilter(Duration.of(5, ChronoUnit.SECONDS), 0.001);
    private final Instrument instrument;
    private final TheoreticalPriceCalculator priceCalculator;
    private TimeAndPrice vwapPrice = new TimeAndPrice(0, Price.EMPTY, VWAP);
    private TimeAndPrice lastPrice = new TimeAndPrice(0, Price.EMPTY, LAST_PRICE);
    private TimeAndPrice bidPrice = new TimeAndPrice(0, Price.EMPTY, BID_PRICE);
    private TimeAndPrice askPrice = new TimeAndPrice(0, Price.EMPTY, ASK_PRICE);
    private TimeAndPrice previousSettlementPrice = new TimeAndPrice(0, Price.EMPTY, SETTLEMENT);

    public PriceSnapshotCalculator(Instrument instrument, TheoreticalPriceCalculator priceCalculator) {
        this.instrument = instrument;
        this.priceCalculator = priceCalculator;
    }

    public MarketDataPrice updateAndGet(Trade trade) {
        Price vwap = vwapCalculator.updateAndGetVwap(trade);
        vwapPrice = vwapPrice.from(trade.timeOfEventMs(), vwap);
        lastPrice = lastPrice.from(trade.timeOfEventMs(), trade.price());
        return getPrice();
    }

    public MarketDataPrice updateAndGet(PriceQuote priceQuote) {
        switch (priceQuote.side()) {
            case BID -> bidPrice = bidPrice.from(priceQuote.timeOfEventMs(), priceQuote.price());
            case ASK -> askPrice = askPrice.from(priceQuote.timeOfEventMs(), priceQuote.price());
        }

        return getPrice();
    }

    public MarketDataPrice getPrice() {
        var timeAndPrice = selectPrice();
        if (throttleFilter.filter(timeAndPrice)) {
            return null;
        }

        Instant instant = Instant.ofEpochSecond(timeAndPrice.timeOfPriceMs);
        LocalDateTime time = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
        return ImmutableMarketDataPrice.builder()
                .priceType(timeAndPrice.priceType)
                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(time).build())
                .price(timeAndPrice.price)
                .build();
    }

    private TimeAndPrice selectPrice() {
        for (var priceType : instrument.priceModelParameters().intradayPricePriority()) {
            var timeAndPrice = getPrice(priceType);
            if (timeAndPrice != null && timeAndPrice.isValid()) {
                return timeAndPrice;
            }
        }
        return previousSettlementPrice;
    }

    private TimeAndPrice getPrice(PriceType priceType) {
        return switch (priceType) {
            case LAST_PRICE -> lastPrice;
            case BID_PRICE -> bidPrice;
            case ASK_PRICE -> askPrice;
            case VWAP -> vwapPrice;
            case MID_BID_ASK_PRICE -> {
                if (bidPrice.isValid() && askPrice.isValid()) {
                    yield new TimeAndPrice(Instant.now().toEpochMilli(), bidPrice.price().add(askPrice.price()).divide(2), MID_BID_ASK_PRICE);
                }
                yield TimeAndPrice.createInvalidPrice();
            }
            case THEORETICAL -> {
                var result = priceCalculator.calculatePrice(instrument);
                if (result.status() == Status.OK) {
                    yield new TimeAndPrice(Instant.now().toEpochMilli(), result.price(), THEORETICAL);
                }
                yield TimeAndPrice.createInvalidPrice();
            }
            default -> throw new IllegalArgumentException(String.format("Price type not %s supported.", priceType));
        };
    }

    public record TimeAndPrice(long timeOfPriceMs, Price price, PriceType priceType) {
        boolean isValid() {
            return price != Price.EMPTY;
        }

        public TimeAndPrice from(long timeOfPrice, Price price) {
            if (timeOfPrice < this.timeOfPriceMs) {
                return this;
            }
            return new TimeAndPrice(timeOfPrice, price, priceType);
        }

        public static TimeAndPrice createInvalidPrice() {
            return new TimeAndPrice(0, Price.EMPTY, null);
        }
    }
}
