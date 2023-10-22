package com.herron.exchange.pricingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaDataConsumer;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.messages.trading.PriceQuote;
import com.herron.exchange.pricingengine.server.PricingEngine;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;


public class TopOfBookConsumer extends KafkaDataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopOfBookConsumer.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.TOP_OF_BOOK_QUOTE, 0);
    private final PricingEngine pricingEngine;

    public TopOfBookConsumer(PricingEngine pricingEngine, MessageFactory messageFactory) {
        super(messageFactory);
        this.pricingEngine = pricingEngine;
    }

    @KafkaListener(id = "pricing-engine-top-of-book-consumer-0",
            topicPartitions = {
                    @TopicPartition(topic = "top-of-book", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
            }
    )
    public void consumerTradeDAtaPartitionZero(ConsumerRecord<String, String> consumerRecord) {
        var broadCastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        if (broadCastMessage != null) {
            handleMessage(broadCastMessage.message());
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof PriceQuote priceQuote) {
            pricingEngine.queueQuote(priceQuote);

        } else if (message instanceof DataStreamState state) {
            switch (state.state()) {
                case START -> LOGGER.info("Started consuming top of book.");
                case DONE -> LOGGER.info("Done consuming {} top of book.", getTotalNumberOfEvents());
            }
        }
    }
}
