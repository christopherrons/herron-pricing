package com.herron.exchange.pricingengine.server.config;

import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.integrations.generator.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.generator.eurex.model.EurexApiClientProperties;
import com.herron.exchange.pricingengine.PricingEngineBootloader;
import com.herron.exchange.pricingengine.server.marketdata.EurexPreviousDaySettlementHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class PricingEngineConfig {

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
    public PricingEngineBootloader pricingEngineBootloader(KafkaBroadcastHandler kafkaBroadcastHandler, EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler) {
        return new PricingEngineBootloader(eurexPreviousDaySettlementHandler, kafkaBroadcastHandler);
    }
}
