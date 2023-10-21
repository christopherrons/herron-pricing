package com.herron.exchange.pricingengine.server.snapshot;


import com.herron.exchange.common.api.common.api.Event;
import com.herron.exchange.common.api.common.api.marketdata.MarketDataPrice;
import com.herron.exchange.common.api.common.api.referencedata.instruments.DerivativeInstrument;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.trading.orders.PriceQuote;
import com.herron.exchange.common.api.common.api.trading.trades.Trade;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.comparator.EventComparator;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.wrappers.ThreadWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class PriceSnapshotHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceSnapshotHandler.class);
    private static final PartitionKey REAL_TIME_PRICE_KEY = new PartitionKey(KafkaTopicEnum.REAL_TIME_PRICES, 0);
    private final Map<Instrument, PriceSnapshotCalculator> instrumentToPriceSnapshotCalculator = new ConcurrentHashMap<>();
    private final Map<String, List<DerivativeInstrument>> underlyingIdToDerivative = new ConcurrentHashMap<>();
    private final BlockingQueue<Event> eventQueue = new PriorityBlockingQueue<>(50, new EventComparator<>());
    private final ScheduledExecutorService queueLoggerThread;
    private final AtomicBoolean isMatching = new AtomicBoolean(false);
    private final Thread pricingThread;
    private final KafkaBroadcastHandler broadcastHandler;

    public PriceSnapshotHandler(String id, KafkaBroadcastHandler broadcastHandler) {
        this.broadcastHandler = broadcastHandler;
        this.pricingThread = new Thread(this::broadcastPrices, id);
        this.queueLoggerThread = newScheduledThreadPool(1, new ThreadWrapper(id));
    }

    public void queueEvent(Event event) {
        eventQueue.add(event);
    }

    public void init() {
        isMatching.set(true);
        pricingThread.start();
        queueLoggerThread.scheduleAtFixedRate(() -> LOGGER.info("Message Queue size: {}", eventQueue.size()), 0, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        LOGGER.info("Stopping snapshot handler.");
        isMatching.set(false);
        queueLoggerThread.shutdown();
    }

    public void handleTrade(Trade trade) {
        eventQueue.add(trade);
    }

    public void handlePriceQuote(PriceQuote priceQuote) {
        eventQueue.add(priceQuote);
    }

    private void broadcastPrices() {
        LOGGER.info("Starting pricing engine.");
        Event event;
        while (isMatching.get() || !eventQueue.isEmpty()) {

            event = poll();
            if (event == null) {
                continue;
            }

            try {
                updateSnapshot(event);
            } catch (Exception e) {
                LOGGER.warn("Unhandled exception for Event: {}", event, e);
            }
        }
    }

    private Event poll() {
        try {
            return eventQueue.poll(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    private void updateSnapshot(Event event) {
        if (event instanceof Trade trade) {
            var instrument = ReferenceDataCache.getCache().getInstrument(trade.instrumentId());
            var calculator = getOrCreateCalculator(instrument);
            var marketPrice = calculator.updateAndGet(trade);
            broadcastPrice(marketPrice);


        } else if (event instanceof PriceQuote priceQuote) {
            var instrument = ReferenceDataCache.getCache().getOrderbookData(priceQuote.orderbookId()).instrument();
            var calculator = getOrCreateCalculator(instrument);
            var marketPrice = calculator.updateAndGet(priceQuote);
            broadcastPrice(marketPrice);
        }
    }

    private void broadcastPrice(MarketDataPrice price) {
        if (price == null) {
            return;
        }
        broadcastHandler.broadcastMessage(REAL_TIME_PRICE_KEY, price);
    }

    public PriceSnapshotCalculator getOrCreateCalculator(Instrument instrument) {
        if (instrument instanceof DerivativeInstrument derivativeInstrument) {
            underlyingIdToDerivative.computeIfAbsent(derivativeInstrument.underlyingInstrumentId(), k -> new ArrayList<>()).add(derivativeInstrument);
        }
        return instrumentToPriceSnapshotCalculator.computeIfAbsent(instrument, PriceSnapshotCalculator::new);
    }
}
