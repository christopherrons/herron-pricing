package com.herron.exchange.pricingengine.server;

import com.herron.exchange.common.api.common.bootloader.Bootloader;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.enums.MarketDataRequestTimeFilter;
import com.herron.exchange.common.api.common.kafka.KafkaBroadcastHandler;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.requests.ImmutableMarketDataPriceRequest;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.pricingengine.server.consumers.ReferenceDataConsumer;
import com.herron.exchange.pricingengine.server.consumers.TopOfBookConsumer;
import com.herron.exchange.pricingengine.server.consumers.TradeDataConsumer;
import com.herron.exchange.pricingengine.server.marketdata.MarketDataService;
import com.herron.exchange.pricingengine.server.theoretical.TheoreticalPriceCalculator;

import java.time.LocalDate;

import static com.herron.exchange.common.api.common.enums.PriceType.THEORETICAL;
import static com.herron.exchange.common.api.common.enums.Status.ERROR;
import static com.herron.exchange.common.api.common.enums.Status.OK;

public class PricingEngineBootloader extends Bootloader {
    public static final PartitionKey PREVIOUS_SETTLEMENT_PRICE_KEY = new PartitionKey(KafkaTopicEnum.PREVIOUS_SETTLEMENT_PRICE_DATA, 0);
    private final MarketDataService marketDataService;
    private final TheoreticalPriceCalculator theoreticalPriceCalculator;
    private final KafkaBroadcastHandler kafkaBroadcastHandler;
    private final ReferenceDataConsumer referenceDataConsumer;
    private final TopOfBookConsumer topOfBookConsumer;
    private final TradeDataConsumer tradeDataConsumer;

    public PricingEngineBootloader(MarketDataService marketDataService,
                                   TheoreticalPriceCalculator theoreticalPriceCalculator,
                                   KafkaBroadcastHandler kafkaBroadcastHandler,
                                   ReferenceDataConsumer referenceDataConsumer,
                                   TopOfBookConsumer topOfBookConsumer,
                                   TradeDataConsumer tradeDataConsumer) {
        super("Pricing-Engine");
        this.marketDataService = marketDataService;
        this.theoreticalPriceCalculator = theoreticalPriceCalculator;
        this.kafkaBroadcastHandler = kafkaBroadcastHandler;
        this.referenceDataConsumer = referenceDataConsumer;
        this.topOfBookConsumer = topOfBookConsumer;
        this.tradeDataConsumer = tradeDataConsumer;
    }

    @Override
    protected void bootloaderInit() {
        referenceDataConsumer.init();
        referenceDataConsumer.await();
        marketDataService.init();
        broadcastPreviousDaySettlement();
        topOfBookConsumer.init();
        tradeDataConsumer.init();
        bootloaderComplete();
    }

    private void broadcastPreviousDaySettlement() {
        for (var instrument : ReferenceDataCache.getCache().getInstruments()) {
            var previousTradingDate = instrument.product().businessCalendar().getFirstDateBeforeHoliday(LocalDate.now());
            var request = ImmutableMarketDataPriceRequest.builder()
                    .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                    .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.from(previousTradingDate)).build())
                    .timeFilter(MarketDataRequestTimeFilter.MATCH_OR_FIRST_PRIOR)
                    .build();
            var response = marketDataService.getMarketDataPrice(request);
            MarketDataPrice marketDataPrice;
            if (response.status() == OK) {
                marketDataPrice = response.marketDataPrice();
            } else {
                var result = theoreticalPriceCalculator.calculatePrice(instrument);
                if (result.status() == ERROR) {
                    continue;
                }
                marketDataPrice = ImmutableMarketDataPrice.builder()
                        .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(instrument.instrumentId()).build())
                        .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.from(previousTradingDate)).build())
                        .price(result.price())
                        .priceType(THEORETICAL)
                        .build();
            }

            if (marketDataPrice == null) {
                continue;
            }

            kafkaBroadcastHandler.broadcastMessage(PREVIOUS_SETTLEMENT_PRICE_KEY, marketDataPrice);
        }
        kafkaBroadcastHandler.endBroadCast(PREVIOUS_SETTLEMENT_PRICE_KEY);
    }
}
