package com.herron.exchange.pricingengine.server.consumers;

import com.herron.exchange.common.api.common.api.Message;
import com.herron.exchange.common.api.common.api.kafka.KafkaMessageHandler;
import com.herron.exchange.common.api.common.api.referencedata.instruments.Instrument;
import com.herron.exchange.common.api.common.api.referencedata.orderbook.OrderbookData;
import com.herron.exchange.common.api.common.bootloader.Bootloader;
import com.herron.exchange.common.api.common.cache.ReferenceDataCache;
import com.herron.exchange.common.api.common.enums.KafkaTopicEnum;
import com.herron.exchange.common.api.common.kafka.KafkaConsumerClient;
import com.herron.exchange.common.api.common.kafka.KafkaSubscriptionRequest;
import com.herron.exchange.common.api.common.messages.BroadcastMessage;
import com.herron.exchange.common.api.common.messages.common.DataStreamState;
import com.herron.exchange.common.api.common.messages.common.PartitionKey;
import com.herron.exchange.common.api.common.messages.refdata.Market;
import com.herron.exchange.common.api.common.messages.refdata.Product;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.herron.exchange.common.api.common.enums.BootloaderStatus.COMPLETE;


public class ReferenceDataConsumerBootloader extends Bootloader implements KafkaMessageHandler {
    private final CountDownLatch countDownLatch;
    private final KafkaConsumerClient consumerClient;
    private final List<KafkaSubscriptionRequest> requests;

    public ReferenceDataConsumerBootloader(CountDownLatch countDownLatch,
                                           KafkaConsumerClient consumerClient) {
        super("Reference Data Bootloader", countDownLatch);
        this.countDownLatch = countDownLatch;
        this.consumerClient = consumerClient;
        this.requests = List.of(
                new KafkaSubscriptionRequest("pricing", new PartitionKey(KafkaTopicEnum.REFERENCE_DATA, 0), this, 0, 1000)
        );
    }

    @Override
    public void bootloaderInit() {
        requests.forEach(consumerClient::subscribeToBroadcastTopic);
    }

    @Override
    public void bootloaderComplete() {
        var count = countDownLatch.getCount();
        countDownLatch.countDown();
        logger.info("Done consuming reference data, countdown latch from {} to {}.", count, countDownLatch.getCount());
        requests.forEach(request -> consumerClient.stop(request.partitionKey()));
        bootloaderStatus = COMPLETE;
    }

    @Override
    public void onMessage(BroadcastMessage broadcastMessage) {
        handleMessage(broadcastMessage.message());
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
                case START -> logger.info("Started consuming reference data.");
                case DONE -> bootloaderComplete();
            }
        }
    }
}
