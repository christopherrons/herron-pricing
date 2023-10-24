package com.herron.exchange.pricingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.kafka.KafkaMessageHandler;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.consumer.DataConsumer;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionDetails;
import com.herron.exchange.common.api.common.kafka.model.KafkaSubscriptionRequest;
import com.herron.exchange.common.api.common.messages.BroadcastMessage;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.common.api.common.messages.refdata.Market;
import com.herron.exchange.common.api.common.messages.refdata.Product;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.herron.exchange.common.api.common.enums.DataStreamEnum.DONE;


public class ReferenceDataConsumer extends DataConsumer implements KafkaMessageHandler {
    private final KafkaConsumerClient consumerClient;
    private final List<KafkaSubscriptionRequest> requests;

    public ReferenceDataConsumer(KafkaConsumerClient consumerClient, List<KafkaSubscriptionDetails> subscriptionDetails) {
        super("Reference Data", new CountDownLatch(subscriptionDetails.size()));
        this.consumerClient = consumerClient;
        this.requests = subscriptionDetails.stream().map(d -> new KafkaSubscriptionRequest(d, this)).toList();
    }

    @Override
    public void consumerInit() {
        requests.forEach(consumerClient::subscribeToBroadcastTopic);
    }

    @Override
    public void consumerComplete() {
        logger.info("Done consuming reference data");
        consumerStatus = DONE;
        shutdown();
    }

    @Override
    public void onMessage(BroadcastMessage broadcastMessage) {
        Message message = broadcastMessage.message();
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
                case START -> logger.info("Started consuming reference data.");
                case DONE -> {
                    consumerClient.stop(broadcastMessage.partitionKey());
                    countDownLatch.countDown();
                    if (countDownLatch.getCount() == 0) {
                        consumerComplete();
                    }
                }
            }
        }
    }
}
