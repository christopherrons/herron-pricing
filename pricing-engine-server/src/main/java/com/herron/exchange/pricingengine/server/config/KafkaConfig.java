package com.herron.exchange.pricingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastProducer;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionDetails;
import com.herron.exchange.common.api.common.logging.EventLogger;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.herron.exchange.pricingengine.server.PricingEngineBootloader.PREVIOUS_SETTLEMENT_PRICE_KEY;
import static com.herron.exchange.pricingengine.server.snapshot.PriceSnapshotHandler.REAL_TIME_PRICE_KEY;

@Configuration
public class KafkaConfig {
    private static final String GROUP_ID = "pricing-engine";

    @Bean
    public KafkaBroadcastHandler kafkaBroadcastHandler(KafkaTemplate<String, Object> kafkaTemplate, KafkaConfig.KafkaProducerConfig config) {
        return new KafkaBroadcastHandler(kafkaTemplate,
                Stream.of(PREVIOUS_SETTLEMENT_PRICE_KEY, REAL_TIME_PRICE_KEY)
                        .map(k -> config.createBroadcastProducer(k, kafkaTemplate))
                        .collect(Collectors.toMap(KafkaBroadcastProducer::getPartitionKey, k -> k))
        );
    }

    @Bean
    public KafkaConsumerClient kafkaConsumerClient(MessageFactory messageFactory, ConsumerFactory<String, String> consumerFactor) {
        return new KafkaConsumerClient(messageFactory, consumerFactor);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic previousSettlementPriceTopic(@Value("${kafka.producer.topic.previous-settlement-price-data.nr-of-partitions:1}") int nrOfPartitions) {
        return TopicBuilder
                .name(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA.getTopicName())
                .partitions(nrOfPartitions)
                .build();
    }

    @Bean
    public NewTopic realTimePriceTopic(@Value("${kafka.producer.topic.real-time-prices.nr-of-partitions:1}") int nrOfPartitions) {
        return TopicBuilder
                .name(KafkaTopicEnum.REAL_TIME_PRICES.getTopicName())
                .partitions(nrOfPartitions)
                .build();
    }

    @Component
    @ConfigurationProperties(prefix = "kafka.producer.broadcast")
    public static class KafkaProducerConfig {

        private List<KafkaTopicConfig> config;

        public List<KafkaTopicConfig> getConfig() {
            return config;
        }

        public void setConfig(List<KafkaTopicConfig> config) {
            this.config = config;
        }

        public record KafkaTopicConfig(int partition,
                                       int eventLogging,
                                       String topic) {
        }

        KafkaBroadcastProducer createBroadcastProducer(PartitionKey partitionKey, KafkaTemplate<String, Object> kafkaTemplate) {
            return config.stream()
                    .filter(c -> c.topic.equals(partitionKey.topicEnum().getTopicName()) && c.partition == partitionKey.partitionId())
                    .map(c -> new KafkaBroadcastProducer(partitionKey, kafkaTemplate, new EventLogger(partitionKey.toString(), c.eventLogging)))
                    .findFirst().orElse(null);
        }
    }

    @Component
    @ConfigurationProperties(prefix = "kafka.consumer")
    public static class KafkaConsumerConfig {

        private List<KafkaTopicConfig> config;

        public List<KafkaTopicConfig> getConfig() {
            return config;
        }

        public void setConfig(List<KafkaTopicConfig> config) {
            this.config = config;
        }

        public record KafkaTopicConfig(int offset,
                                       int partition,
                                       int eventLogging,
                                       String topic) {
        }

        List<KafkaSubscriptionDetails> getDetails(KafkaTopicEnum topicEnum) {
            return config.stream()
                    .filter(c -> c.topic.equals(topicEnum.getTopicName()))
                    .map(c -> new KafkaSubscriptionDetails(GROUP_ID, new PartitionKey(topicEnum, c.partition), c.offset, c.eventLogging))
                    .toList();
        }
    }
}
