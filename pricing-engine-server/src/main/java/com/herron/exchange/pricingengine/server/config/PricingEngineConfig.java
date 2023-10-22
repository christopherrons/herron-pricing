package com.herron.exchange.pricingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.integrations.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.eurex.model.EurexApiClientProperties;
import com.herron.exchange.pricingengine.server.PricingEngineBootloader;
import com.herron.exchange.pricingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.EurexPreviousDaySettlementHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public MarketDataHandler marketDataHandler(ExternalMarketDataHandler externalMarketDataHandler) {
        return new MarketDataHandler(externalMarketDataHandler);
    }

    @Bean
    public CountDownLatch referenceDataCountdownLatch() {
        return new CountDownLatch(1);
    }

    @Bean
    public ReferenceDataConsumer referenceDataConsumer(CountDownLatch countDownLatch, MessageFactory messageFactory) {
        return new ReferenceDataConsumer(countDownLatch, messageFactory);
    }

    @Bean(initMethod = "init")
    public PricingEngineBootloader pricingEngineBootloader(KafkaBroadcastHandler kafkaBroadcastHandler, MarketDataHandler marketDataHandler) {
        return new PricingEngineBootloader(marketDataHandler, kafkaBroadcastHandler);
    }
}
