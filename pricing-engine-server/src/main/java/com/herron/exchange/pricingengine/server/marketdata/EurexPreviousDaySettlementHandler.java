package com.herron.exchange.pricingengine.server.marketdata;

import com.herron.exchange.common.api.common.api.marketdata.MarketDataPrice;
import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultMarketDataPrice;
import com.herron.exchange.common.api.common.model.ImmutableDefaultMarketDataPriceStaticKey;
import com.herron.exchange.common.api.common.model.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.integrations.generator.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.generator.eurex.model.EurexContractData;
import com.herron.exchange.integrations.generator.eurex.model.EurexProductData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class EurexPreviousDaySettlementHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EurexPreviousDaySettlementHandler.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private EurexReferenceDataApiClient client;

    public EurexPreviousDaySettlementHandler(EurexReferenceDataApiClient client) {
        this.client = client;
    }

    public List<MarketDataPrice> getPreviousDaySettlementPrices() {
        LOGGER.info("Fetching Eurex previous day settlement prices.");

        EurexProductData eurexProductData = client.fetchProductData();
        List<EurexContractData> eurexContractDataList = client.fetchContractData(eurexProductData);
        if (eurexContractDataList.isEmpty()) {
            return List.of();
        }

        LOGGER.info("Done fetching Eurex previous day settlement prices.");
        List<MarketDataPrice> previousSettlementPrice = new ArrayList<>();
        for (var contractData : eurexContractDataList) {
            var previousDate = LocalDateTime.parse(contractData.data().contracts().date(), DATE_TIME_FORMATTER).minusDays(1);
            for (var contract : contractData.data().contracts().data()) {
                previousSettlementPrice.add(
                        ImmutableDefaultMarketDataPrice.builder().
                                priceType(PriceType.SETTLEMENT)
                                .staticKey(ImmutableDefaultMarketDataPriceStaticKey.builder().instrumentId(contract.isin()).build())
                                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(previousDate).build())
                                .build()
                );
            }
        }

        return previousSettlementPrice;
    }
}
