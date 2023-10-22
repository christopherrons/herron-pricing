package com.herron.exchange.pricingengine.server;

import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PricingEngineBootloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PricingEngineBootloader.class);
    private static final PartitionKey PREVIOUS_SETTLEMENT_PRICE_KEY = new PartitionKey(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA, 0);
    private final MarketDataService marketDataService;
    private final KafkaBroadcastHandler kafkaBroadcastHandler;
    private final Thread bootloaderThread;

    public PricingEngineBootloader(MarketDataService marketDataService,
                                   KafkaBroadcastHandler kafkaBroadcastHandler) {
        this.marketDataService = marketDataService;
        this.kafkaBroadcastHandler = kafkaBroadcastHandler;
        this.bootloaderThread = new Thread(this::run);
    }

    public void init() {
        bootloaderThread.start();
    }

    private void run() {
        broadCastPreviousDaySettlement();
    }

    private void broadCastPreviousDaySettlement() {
        marketDataService.getPreviousDaySettlementPrices()
                .forEach(message -> kafkaBroadcastHandler.broadcastMessage(PREVIOUS_SETTLEMENT_PRICE_KEY, message));
        kafkaBroadcastHandler.endBroadCast(PREVIOUS_SETTLEMENT_PRICE_KEY);
    }
}
