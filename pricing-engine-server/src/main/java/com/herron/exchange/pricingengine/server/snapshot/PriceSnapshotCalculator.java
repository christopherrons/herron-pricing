package com.herron.exchange.pricingengine.server.snapshot;


import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.enums.Status;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.trading.TopOfBook;
import com.herron.exchange.common.api.common.messages.trading.Trade;
import com.herron.exchange.pricingengine.server.theoretical.TheoreticalPriceCalculator;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.herron.exchange.common.api.common.enums.PriceType.*;

public class PriceSnapshotCalculator {

    private final VwapCalculator vwapCalculator = new VwapCalculator();
    private final SnapshotPriceThrottleFilter throttleFilter = new SnapshotPriceThrottleFilter(Duration.of(5, ChronoUnit.SECONDS), 0.001);
    private final Instrument instrument;
    private final TheoreticalPriceCalculator priceCalculator;
    private TimeAndPrice vwapPrice = new TimeAndPrice(Timestamp.from(0), Price.EMPTY, VWAP);
    private TimeAndPrice lastPrice = new TimeAndPrice(Timestamp.from(0), Price.EMPTY, LAST_PRICE);
    private TimeAndPrice bidPrice = new TimeAndPrice(Timestamp.from(0), Price.EMPTY, BID_PRICE);
    private TimeAndPrice askPrice = new TimeAndPrice(Timestamp.from(0), Price.EMPTY, ASK_PRICE);

    public PriceSnapshotCalculator(Instrument instrument, TheoreticalPriceCalculator priceCalculator) {
        this.instrument = instrument;
        this.priceCalculator = priceCalculator;
    }

    public MarketDataPrice updateAndGet(Trade trade) {
        Price vwap = vwapCalculator.updateAndGetVwap(trade);
        vwapPrice = vwapPrice.from(trade.timeOfEvent(), vwap);
        lastPrice = lastPrice.from(trade.timeOfEvent(), trade.price());
        return getPrice();
    }

    public MarketDataPrice updateAndGet(TopOfBook topOfBook) {
        bidPrice = Optional.ofNullable(topOfBook.bidQuote()).map(bq -> bidPrice.from(bq.timeOfEvent(), bq.price())).orElse(bidPrice);
        askPrice = Optional.ofNullable(topOfBook.askQuote()).map(aq -> askPrice.from(aq.timeOfEvent(), aq.price())).orElse(askPrice);
        return getPrice();
    }

    public MarketDataPrice getPrice() {
        var timeAndPrice = selectPrice();
        if (throttleFilter.filter(timeAndPrice)) {
            return null;
        }

        return ImmutableMarketDataPrice.builder()
                .priceType(timeAndPrice.priceType)
                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(timeAndPrice.timestamp).build())
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
        return null;
    }

    private TimeAndPrice getPrice(PriceType priceType) {
        return switch (priceType) {
            case LAST_PRICE -> lastPrice;
            case BID_PRICE -> bidPrice;
            case ASK_PRICE -> askPrice;
            case VWAP -> vwapPrice;
            case MID_BID_ASK_PRICE -> {
                if (bidPrice.isValid() && askPrice.isValid()) {
                    yield new TimeAndPrice(Timestamp.now(), bidPrice.price().add(askPrice.price()).divide(2), MID_BID_ASK_PRICE);
                }
                yield TimeAndPrice.createInvalidPrice();
            }
            case THEORETICAL -> {
                var result = priceCalculator.calculatePrice(instrument, Timestamp.now());
                if (result.status() == Status.OK) {
                    yield new TimeAndPrice(result.calculationTime(), result.price(), THEORETICAL);
                }
                yield TimeAndPrice.createInvalidPrice();
            }
            default -> throw new IllegalArgumentException(String.format("Price type not %s supported.", priceType));
        };
    }

    public record TimeAndPrice(Timestamp timestamp, Price price, PriceType priceType) {
        public static TimeAndPrice createInvalidPrice() {
            return new TimeAndPrice(Timestamp.from(0), Price.EMPTY, null);
        }

        boolean isValid() {
            return price != Price.EMPTY;
        }

        public TimeAndPrice from(Timestamp timestamp, Price price) {
            if (timestamp.isBefore(this.timestamp)) {
                return this;
            }
            return new TimeAndPrice(timestamp, price, priceType);
        }
    }
}
