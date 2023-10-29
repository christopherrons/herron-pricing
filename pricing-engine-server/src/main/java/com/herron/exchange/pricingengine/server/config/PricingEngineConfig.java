package com.herron.exchange.pricingengine.server.config;

import com.herron.exchange.common.api.common.api.MessageFactory;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.mapping.DefaultMessageFactory;
import com.herron.exchange.integrations.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.eurex.model.EurexApiClientProperties;
import com.herron.exchange.integrations.nasdaq.NasdaqYieldCurveClient;
import com.herron.exchange.integrations.nasdaq.model.NasdaqDataLinkApiClientProperties;
import com.herron.exchange.pricingengine.server.PricingEngine;
import com.herron.exchange.pricingengine.server.PricingEngineBootloader;
import com.herron.exchange.pricingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.pricingengine.server.consumers.TopOfBookConsumer;
import com.herron.exchange.pricingengine.server.consumers.TradeDataConsumer;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.marketdata.external.ExternalMarketDataHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.eurex.EurexPreviousDaySettlementHandler;
import com.herron.exchange.pricingengine.server.marketdata.external.nasdaq.NasdaqYieldCurveHandler;
import com.herron.exchange.pricingengine.server.theoretical.TheoreticalPriceCalculator;
import com.herron.exchange.pricingengine.server.theoretical.derivatives.futures.FuturesCalculator;
import com.herron.exchange.pricingengine.server.theoretical.derivatives.options.OptionCalculator;
import com.herron.exchange.pricingengine.server.theoretical.fixedincome.bonds.BondPriceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.herron.exchange.common.api.common.enums.KafkaTopicEnum.*;

@Configuration
public class PricingEngineConfig {

    @Bean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    public TheoreticalPriceCalculator theoreticalPriceCalculator(MarketDataService marketDataService) {
        return new TheoreticalPriceCalculator(
                new BondPriceCalculator(marketDataService),
                new OptionCalculator(marketDataService),
                new FuturesCalculator(marketDataService)
        );
    }

    @Bean
    public PricingEngine pricingEngine(KafkaBroadcastHandler broadcastHandler, TheoreticalPriceCalculator theoreticalPriceCalculator) {
        return new PricingEngine(broadcastHandler, theoreticalPriceCalculator);
    }

    @Bean
    public EurexApiClientProperties eurexApiClientProperties(@Value("${market-data.external.eurex.api-key}") String apiKey,
                                                             @Value("${market-data.external.eurex.api-url}") String url,
                                                             @Value("${market-data.external.eurex.contractRequestLimit}") int contractRequestLimit) {
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
    public NasdaqDataLinkApiClientProperties nasdaqDataLinkApiClientProperties(@Value("${market-data.nasdaq.eurex.api-key}") String apiKey,
                                                                               @Value("${market-data.external.nasdaq.api-url}") String url) {
        return new NasdaqDataLinkApiClientProperties(url, apiKey);
    }

    @Bean
    public NasdaqYieldCurveClient nasdaqYieldCurveClient(NasdaqDataLinkApiClientProperties nasdaqDataLinkApiClientProperties) {
        return new NasdaqYieldCurveClient(nasdaqDataLinkApiClientProperties);
    }

    @Bean
    public NasdaqYieldCurveHandler nasdaqYieldCurveHandler(NasdaqYieldCurveClient nasdaqYieldCurveClient) {
        return new NasdaqYieldCurveHandler(nasdaqYieldCurveClient);
    }

    @Bean
    public ExternalMarketDataHandler externalMarketDataHandler(EurexPreviousDaySettlementHandler eurexPreviousDaySettlementHandler,
                                                               NasdaqYieldCurveHandler nasdaqYieldCurveHandler) {
        return new ExternalMarketDataHandler(eurexPreviousDaySettlementHandler, nasdaqYieldCurveHandler);
    }

    @Bean
    public MarketDataService marketDataHandler(ExternalMarketDataHandler externalMarketDataHandler) {
        return new MarketDataService(externalMarketDataHandler);
    }

    @Bean
    public ReferenceDataConsumer referenceDataConsumer(KafkaConsumerClient kafkaConsumerClient, KafkaConfig.KafkaConsumerConfig config) {
        return new ReferenceDataConsumer(kafkaConsumerClient, config.getDetails(REFERENCE_DATA));
    }

    @Bean
    public TopOfBookConsumer topOfBookConsumer(PricingEngine pricingEngine, KafkaConsumerClient kafkaConsumerClient, KafkaConfig.KafkaConsumerConfig config) {
        return new TopOfBookConsumer(pricingEngine, kafkaConsumerClient, config.getDetails(TOP_OF_BOOK_QUOTE));
    }

    @Bean
    public TradeDataConsumer tradeDataConsumer(PricingEngine pricingEngine, KafkaConsumerClient kafkaConsumerClient, KafkaConfig.KafkaConsumerConfig config) {
        return new TradeDataConsumer(pricingEngine, kafkaConsumerClient, config.getDetails(TRADE_DATA));
    }

    @Bean(initMethod = "init")
    public PricingEngineBootloader pricingEngineBootloader(KafkaBroadcastHandler kafkaBroadcastHandler,
                                                           TheoreticalPriceCalculator theoreticalPriceCalculator,
                                                           MarketDataService marketDataService,
                                                           ReferenceDataConsumer referenceDataConsumer,
                                                           TradeDataConsumer tradeDataConsumer,
                                                           TopOfBookConsumer topOfBookConsumer) {
        return new PricingEngineBootloader(marketDataService, theoreticalPriceCalculator, kafkaBroadcastHandler, referenceDataConsumer, topOfBookConsumer, tradeDataConsumer);
    }
}
