package com.herron.exchange.pricingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.integrations.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.eurex.model.EurexApiClientProperties;
import com.herron.exchange.pricingengine.server.PricingEngine;
import com.herron.exchange.pricingengine.server.PricingEngineBootloader;
import com.herron.exchange.pricingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.pricingengine.server.consumers.TopOfBookConsumer;
import com.herron.exchange.pricingengine.server.consumers.TradeDataConsumer;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.marketdata.external.EurexPreviousDaySettlementHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CountDownLatch;

@Configuration
public class PricingEngineConfig {

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    public KafkaBroadcastHandler kafkaBroadcastHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaBroadcastHandler(kafkaTemplate);
    }

    @Bean
    public EurexApiClientProperties eurexApiClientProperties(@Value("${reference-data.external.eurex.api-key}") String apiKey,
                                                             @Value("${reference-data.external.eurex.api-url}") String url,
                                                             @Value("${reference-data.external.eurex.contractRequestLimit}") int contractRequestLimit) {
        return new EurexApiClientProperties(url, apiKey, contractRequestLimit);
    }

    @Bean
    public EurexReferenceDataApiClient eurexReferenceDataApiClient(EurexApiClientProperties eurexApiClientProperties) {
        return new EurexReferenceDataApiClient(eurexApiClientProperties);
    }

    @Bean
    public EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler(EurexReferenceDataApiClient eurexReferenceDataApiClient) {
        return new EurexPreviousDaySettlementHandler(eurexReferenceDataApiClient);
    }

    @Bean
    public ExternalMarketDataHandler externalMarketDataHandler(EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler) {
        return new ExternalMarketDataHandler(eurexPreviousDaySettlementHandler);
    }

    @Bean
    public MarketDataService marketDataHandler(ExternalMarketDataHandler externalMarketDataHandler) {
        return new MarketDataService(externalMarketDataHandler);
    }

    @Bean
    public CountDownLatch referenceDataCountdownLatch() {
        return new CountDownLatch(1);
    }

    @Bean
    public KafkaConsumerClient kafkaConsumerClient(MessageFactory messageFactory, ConsumerFactory<String, String> consumerFactor) {
        return new KafkaConsumerClient(messageFactory, consumerFactor);
    }

    @Bean
    public ReferenceDataConsumer referenceDataConsumer(KafkaConsumerClient kafkaConsumerClient) {
        return new ReferenceDataConsumer(kafkaConsumerClient);
    }

    @Bean
    public TopOfBookConsumer topOfBookConsumer(PricingEngine pricingEngine, KafkaConsumerClient kafkaConsumerClient) {
        return new TopOfBookConsumer(pricingEngine, kafkaConsumerClient);
    }

    @Bean
    public TradeDataConsumer tradeDataConsumer(PricingEngine pricingEngine, KafkaConsumerClient kafkaConsumerClient) {
        return new TradeDataConsumer(pricingEngine, kafkaConsumerClient);
    }

    @Bean(initMethod = "init")
    public PricingEngineBootloader pricingEngineBootloader(KafkaBroadcastHandler kafkaBroadcastHandler,
                                                           MarketDataService marketDataService,
                                                           ReferenceDataConsumer referenceDataConsumer,
                                                           TradeDataConsumer tradeDataConsumer,
                                                           TopOfBookConsumer topOfBookConsumer) {
        return new PricingEngineBootloader(marketDataService, kafkaBroadcastHandler, referenceDataConsumer, topOfBookConsumer, tradeDataConsumer);
    }
}
