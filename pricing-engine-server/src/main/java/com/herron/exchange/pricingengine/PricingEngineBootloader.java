package com.herron.exchange.pricingengine;

import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.model.PartitionKey;
import com.herron.exchange.pricingengine.server.marketdata.EurexPreviousDaySettlementHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PricingEngineBootloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PricingEngineBootloader.class);
    private static final PartitionKey PREVIOUS_SETTLEMENT_PRICE_KEY = new PartitionKey(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA, 0);
    private final EurexPreviousDaySettlementHandler previousDaySettlementHandler;
    private final KafkaBroadcastHandler kafkaBroadcastHandler;

    public PricingEngineBootloader(EurexPreviousDaySettlementHandler previousDaySettlementHandler,
                                   KafkaBroadcastHandler kafkaBroadcastHandler) {
        this.previousDaySettlementHandler = previousDaySettlementHandler;
        this.kafkaBroadcastHandler = kafkaBroadcastHandler;
    }

    public void init() {
        broadCastPreviousDaySettlement();
    }

    private void broadCastPreviousDaySettlement() {
        previousDaySettlementHandler.getPreviousDaySettlementPrices()
                .forEach(message -> kafkaBroadcastHandler.broadcastMessage(PREVIOUS_SETTLEMENT_PRICE_KEY, message));
        kafkaBroadcastHandler.endBroadCast(PREVIOUS_SETTLEMENT_PRICE_KEY);
    }
}
