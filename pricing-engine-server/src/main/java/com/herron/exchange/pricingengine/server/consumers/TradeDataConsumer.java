package com.herron.exchange.pricingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaDataConsumer;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.messages.trading.Trade;
import com.herron.exchange.pricingengine.server.PricingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;


public class TradeDataConsumer extends KafkaDataConsumer {
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.TRADE_DATA, 0);
    private final PricingEngine pricingEngine;

    public TradeDataConsumer(PricingEngine pricingEngine, MessageFactory messageFactory) {
        super(messageFactory);
        this.pricingEngine = pricingEngine;
    }

    @KafkaListener(id = "pricing-engine-trade-data-consumer-0",
            topicPartitions = {
                    @TopicPartition(topic = "trade-data", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
            }
    )
    public void consumerTradeDAtaPartitionZero(ConsumerRecord<String, String> consumerRecord) {
        var broadCastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        if (broadCastMessage != null) {
            handleMessage(broadCastMessage.message());
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof Trade trade) {
            pricingEngine.queueTrade(trade);

        } else if (message instanceof DataStreamState state) {
            switch (state.state()) {
                case START -> logger.info("Started consuming trade data.");
                case DONE -> logger.info("Done consuming {} trade data.", getTotalNumberOfEvents());
            }
        }
    }
}
