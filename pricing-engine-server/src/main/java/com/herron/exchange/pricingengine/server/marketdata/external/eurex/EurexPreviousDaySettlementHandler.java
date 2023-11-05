package com.herron.exchange.pricingengine.server.marketdata.external.eurex;

import com.herron.exchange.common.api.common.enums.PriceType;
import com.herron.exchange.common.api.common.messages.common.Price;
import com.herron.exchange.common.api.common.messages.common.Timestamp;
import com.herron.exchange.common.api.common.messages.marketdata.ImmutableDefaultTimeComponentKey;
import com.herron.exchange.common.api.common.messages.marketdata.entries.ImmutableMarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.entries.MarketDataPrice;
import com.herron.exchange.common.api.common.messages.marketdata.statickeys.ImmutableMarketDataPriceStaticKey;
import com.herron.exchange.integrations.eurex.EurexReferenceDataApiClient;
import com.herron.exchange.integrations.eurex.model.EurexContractData;
import com.herron.exchange.integrations.eurex.model.EurexProductData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EurexPreviousDaySettlementHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EurexPreviousDaySettlementHandler.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<String, Double> ISIN_PRICE_MAP = isinToPriceMap();
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

        List<MarketDataPrice> previousSettlementPrice = new ArrayList<>();
        for (var contractData : eurexContractDataList) {
            var previousDate = LocalDateTime.of(LocalDate.parse(contractData.data().contracts().date(), DATE_TIME_FORMATTER), LocalTime.MIDNIGHT).minusDays(1);
            for (var contract : contractData.data().contracts().data()) {
                previousSettlementPrice.add(
                        ImmutableMarketDataPrice.builder()
                                .priceType(PriceType.SETTLEMENT)
                                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(contract.isin()).build())
                                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.from(previousDate)).build())
                                .price(Price.create(contract.previousDaySettlementPrice()))
                                .build()
                );
            }
        }
        // There are no underlying prices available from this eurex client
        List<MarketDataPrice> underlyingPrice = mockUnderlyingPrices(eurexProductData, eurexContractDataList);

        LOGGER.info("Done fetching Eurex previous day settlement prices.");
        return Stream.concat(previousSettlementPrice.stream(), underlyingPrice.stream()).toList();
    }

    private List<MarketDataPrice> mockUnderlyingPrices(EurexProductData eurexProductData, List<EurexContractData> eurexContractDataList) {
        Map<String, EurexProductData.ProductInfo> productToInfo = eurexProductData.data().productInfos().data().stream()
                .collect(Collectors.toMap(EurexProductData.ProductInfo::product, Function.identity(), (current, other) -> current));
        Set<EurexProductData.ProductInfo> productInfos = eurexContractDataList.stream()
                .flatMap(k -> k.data().contracts().data().stream())
                .filter(c -> productToInfo.containsKey(c.product()))
                .map(c -> productToInfo.get(c.product()))
                .collect(Collectors.toSet());
        return productInfos.stream()
                .filter(productInfo -> ISIN_PRICE_MAP.containsKey(productInfo.underlyingIsin()))
                .<MarketDataPrice>map(productInfo ->
                        ImmutableMarketDataPrice.builder()
                                .priceType(PriceType.SETTLEMENT)
                                .staticKey(ImmutableMarketDataPriceStaticKey.builder().instrumentId(productInfo.underlyingIsin()).build())
                                .timeComponentKey(ImmutableDefaultTimeComponentKey.builder().timeOfEvent(Timestamp.from(LocalDate.now().minusDays(1))).build())
                                .price(Price.create(ISIN_PRICE_MAP.get(productInfo.underlyingIsin())))
                                .build()
                )
                .toList();
    }

    private static Map<String, Double> isinToPriceMap() {
        Map<String, Double> isingToPrice = new HashMap<>();
        isingToPrice.put("DE000ENAG999", 11.37);
        isingToPrice.put("CA76131D1033", 67.80);
        isingToPrice.put("EU0009658368", 485.72);
        isingToPrice.put("XC000A11Q9E3", 100.0);
        isingToPrice.put("CH0011795959", 422.00);
        isingToPrice.put("US0258161092", 152.76);
        isingToPrice.put("DE0005439004", 62.76);
        isingToPrice.put("FR0000131906", 34.74);
        isingToPrice.put("DE000A1DAHH0", 71.36);
        isingToPrice.put("DE0005557508", 21.39);
        isingToPrice.put("FR0000045072", 11.68);
        isingToPrice.put("XC000A13RM69", 11.37);
        isingToPrice.put("AT0000746409", 84.60);
        isingToPrice.put("NL0013332471", 5.88);
        isingToPrice.put("DE000NWRK013", 72.70);
        isingToPrice.put("BE0974258874", 39.14);
        isingToPrice.put("FR0000124141", 26.85);
        return isingToPrice;
    }

}
