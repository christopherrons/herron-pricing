package com.herron.exchange.pricingengine.server;

import com.herron.exchange.common.api.common.api.Event;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.trading.PriceQuote;
import com.herron.exchange.common.api.common.messages.trading.Trade;
import com.herron.exchange.pricingengine.server.pricemodels.TheoreticalPriceCalculator;
import com.herron.exchange.pricingengine.server.snapshot.PriceSnapshotHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PricingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PricingEngine.class);
    private final Map<String, PriceSnapshotHandler> idToSnapshotHandler = new ConcurrentHashMap<>();
    private final KafkaBroadcastHandler broadcastHandler;
    private final TheoreticalPriceCalculator priceCalculator;

    public PricingEngine(KafkaBroadcastHandler broadcastHandler, TheoreticalPriceCalculator priceCalculator) {
        this.broadcastHandler = broadcastHandler;
        this.priceCalculator = priceCalculator;
    }

    public void queueTrade(Trade trade) {
        String id = ReferenceDataCache.getCache().getInstrument(trade.instrumentId()).product().market().marketId();
        queueMessage(id, trade);
    }

    public void queueQuote(PriceQuote quote) {
        String id = ReferenceDataCache.getCache().getOrderbookData(quote.orderbookId()).instrument().product().market().marketId();
        queueMessage(id, quote);
    }

    private void queueMessage(String id, Event event) {
        idToSnapshotHandler.computeIfAbsent(id, key -> {
                    var snapshotHandler = new PriceSnapshotHandler(key, broadcastHandler, priceCalculator);
                    snapshotHandler.init();
                    return snapshotHandler;
                })
                .queueEvent(event);
    }
}
