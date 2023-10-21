package com.herron.exchange.pricingengine.server.snapshot;


import com.herron.exchange.common.api.common.api.marketdata.MarketDataPrice;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.trading.orders.PriceQuote;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultMarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.pricingengine.server.price.VwapCalculator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.herron.exchange.common.api.common.enums.PriceType.*;

public class PriceSnapshotCalculator {

    private final VwapCalculator vwapCalculator = new VwapCalculator();
    private final Instrument instrument;
    private TimeAndPrice vwapPrice = new TimeAndPrice(0, Price.EMPTY, VWAP);
    private TimeAndPrice lastPrice = new TimeAndPrice(0, Price.EMPTY, LAST_PRICE);
    private TimeAndPrice bidPrice = new TimeAndPrice(0, Price.EMPTY, BID_PRICE);
    private TimeAndPrice askPrice = new TimeAndPrice(0, Price.EMPTY, ASK_PRICE);
    private TimeAndPrice previousSettlementPrice = new TimeAndPrice(0, Price.EMPTY, SETTLEMENT);

    public PriceSnapshotCalculator(Instrument instrument) {
        this.instrument = instrument;
    }

    public MarketDataPrice updateAndGet(Trade trade) {
        Price vwap = vwapCalculator.updateAndGetVwap(trade);
        vwapPrice = vwapPrice.from(trade.timeOfEventMs(), vwap);
        lastPrice = lastPrice.from(trade.timeOfEventMs(), trade.price());
        return getPrice();
    }

    public MarketDataPrice updateAndGet(PriceQuote priceQuote) {
        switch (priceQuote.side()) {
            case BID -> bidPrice.from(priceQuote.timeOfEventMs(), priceQuote.price());
            case ASK -> askPrice.from(priceQuote.timeOfEventMs(), priceQuote.price());
        }
        return getPrice();
    }

    public MarketDataPrice getPrice() {
        var timeAndPrice = selectPrice();
        Instant instant = Instant.ofEpochSecond(timeAndPrice.timeOfPrice);
        LocalDateTime time = instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
        return ImmutableDefaultMarketDataPrice.builder()
                .priceType(timeAndPrice.priceType)
                .staticKey(ImmutableDefaultMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(time).build())
                .price(timeAndPrice.price)
                .build();
    }

    private TimeAndPrice selectPrice() {
        return lastPrice; //FIXME: Add a price selection waterfall + time filter
    }

    private record TimeAndPrice(long timeOfPrice, Price price, PriceType priceType) {
        TimeAndPrice from(long timeOfPrice, Price price) {
            if (timeOfPrice < this.timeOfPrice) {
                return this;
            }
            return new TimeAndPrice(timeOfPrice, price, priceType);
        }
    }
}
