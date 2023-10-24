package com.herron.exchange.pricingengine.server;

import com.herron.exchange.common.api.common.bootloader.Bootloader;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.pricingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.pricingengine.server.consumers.TopOfBookConsumer;
import com.herron.exchange.pricingengine.server.consumers.TradeDataConsumer;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;

public class PricingEngineBootloader extends Bootloader {
    public static final PartitionKey PREVIOUS_SETTLEMENT_PRICE_KEY = new PartitionKey(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA, 0);
    private final MarketDataService marketDataService;
    private final KafkaBroadcastHandler kafkaBroadcastHandler;
    private final ReferenceDataConsumer referenceDataConsumer;
    private final TopOfBookConsumer topOfBookConsumer;
    private final TradeDataConsumer tradeDataConsumer;

    public PricingEngineBootloader(MarketDataService marketDataService,
                                   KafkaBroadcastHandler kafkaBroadcastHandler,
                                   ReferenceDataConsumer referenceDataConsumer,
                                   TopOfBookConsumer topOfBookConsumer,
                                   TradeDataConsumer tradeDataConsumer) {
        super("Pricing-Engine");
        this.marketDataService = marketDataService;
        this.kafkaBroadcastHandler = kafkaBroadcastHandler;
        this.referenceDataConsumer = referenceDataConsumer;
        this.topOfBookConsumer = topOfBookConsumer;
        this.tradeDataConsumer = tradeDataConsumer;
    }

    @Override
    protected void bootloaderInit() {
        broadcastPreviousDaySettlement();
        referenceDataConsumer.init();
        referenceDataConsumer.await();
        topOfBookConsumer.init();
        tradeDataConsumer.init();
        bootloaderComplete();
    }

    private void broadcastPreviousDaySettlement() {
        marketDataService.getPreviousDaySettlementPrices()
                .forEach(message -> kafkaBroadcastHandler.broadcastMessage(PREVIOUS_SETTLEMENT_PRICE_KEY, message));
        kafkaBroadcastHandler.endBroadCast(PREVIOUS_SETTLEMENT_PRICE_KEY);
    }
}
