# Data Flow

This document describes the data flow of the application.

## Table of Content

* [Visual Flow Chart](#visual-flow-chart): Visual Flow Chart.

## Visual Flow Chart

```mermaid
flowchart LR;
subgraph KAFKA_TRADES_TOPIC[Trades]
KAFKA_TRADES_TOPIC_PARTITION_1[Partition 1] -.- KAFKA_TRADES_TOPIC_PARTITION_N[Partition... N]
end

subgraph KAFKA_TOP_OF_BOOK_TOPIC[Top of Book]
KAFKA_TOP_OF_BOOK_TOPIC_PARTITION_1[Partition 1] -.- KAFKA_TOP_OF_BOOK_TOPIC_PARTITION_N[Partition.. N]
end

subgraph KAFKA_REFERENCE_DATA_TOPIC[Reference Data]
KAFKA_REFERENCE_DATA_TOPIC_PARTITION_1[Partition 1] -.- KAFKA_REFERENCE_DATA_TOPIC_PARTITION_N[Partition... N]
end

KAFKA_TRADES_TOPIC -->|kafka subscription|PRICING_ENGINE
KAFKA_TOP_OF_BOOK_TOPIC -->|kafka subscription|PRICING_ENGINE
KAFKA_REFERENCE_DATA_TOPIC -->|kafka subscription|PRICING_ENGINE
subgraph PRICING_ENGINE[Pricing Engine]
end

PRICING_ENGINE -->|Events|PRICE_SNAPSHOT_HANDLER
subgraph PRICE_SNAPSHOT_HANDLER[Price Snapshot Handler]
PRICE_SNAPSHOT_HANDLER_PARTITION_1[Partition 1] -.- PRICE_SNAPSHOT_HANDLER_PARTITION_2[Partition... N]
end

PRICE_SNAPSHOT_HANDLER --> KAFKA_REAL_TIME_PRICES
subgraph KAFKA_REAL_TIME_PRICES[Real Time Prices]
KAFKA_REAL_TIME_PRICES_PARTITION_1[Partition 1] -.- KAFKA_REAL_TIME_PRICES_PARTITION_N[Partition... N]
end

KAFKA_REAL_TIME_PRICES ---> KAFKA_PRODUCER

subgraph KAFKA_PRODUCER[Kafka Producer]
KAFKA_PRODUCER_PARTITION_1[Partition 1] -.- KAFKA_PRODUCER_PARTITION_N[Partition... N]
end
```
