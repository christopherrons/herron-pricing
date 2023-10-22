package com.herron.exchange.pricingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaDataConsumer;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.messages.refdata.Market;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.util.concurrent.CountDownLatch;


public class ReferenceDataConsumer extends KafkaDataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataConsumer.class);
    private static final PartitionKey PARTITION_ZERO_KEY = new PartitionKey(KafkaTopicEnum.REFERENCE_DATA, 0);
    private final CountDownLatch countDownLatch;

    public ReferenceDataConsumer(CountDownLatch countDownLatch, MessageFactory messageFactory) {
        super(messageFactory);
        this.countDownLatch = countDownLatch;
    }

    @KafkaListener(id = "pricing-engine-reference-data-consumer-0",
            topicPartitions = {
                    @TopicPartition(topic = "reference-data", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0"))
            }
    )
    public void consumerReferenceDataPartitionZero(ConsumerRecord<String, String> consumerRecord) {
        var broadCastMessage = deserializeBroadcast(consumerRecord, PARTITION_ZERO_KEY);
        if (broadCastMessage != null) {
            handleMessage(broadCastMessage.message());
        }
    }

    private void handleMessage(Message message) {
        if (message instanceof Market market) {
            ReferenceDataCache.getCache().addMarket(market);

        } else if (message instanceof Product product) {
            ReferenceDataCache.getCache().addProduct(product);

        } else if (message instanceof Instrument instrument) {
            ReferenceDataCache.getCache().addInstrument(instrument);

        } else if (message instanceof OrderbookData orderbookData) {
            ReferenceDataCache.getCache().addOrderbookData(orderbookData);

        } else if (message instanceof DataStreamState state) {
            switch (state.state()) {
                case START -> LOGGER.info("Started consuming reference data.");
                case DONE -> {
                    var count = countDownLatch.getCount();
                    countDownLatch.countDown();
                    LOGGER.info("Done consuming {} reference data, countdown latch from {} to {}.", getTotalNumberOfEvents(), count, countDownLatch.getCount());
                }
            }
        }
    }
}
