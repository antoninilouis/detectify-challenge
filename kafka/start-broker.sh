#!/usr/bin/env bash

# configure kafka broker
cp /kafka/config/server.properties /kafka/config/broker.properties
sed -i "/^zookeeper.connect=/c\zookeeper.connect=${ZOOKEEPER_HOST}:2181" /kafka/config/broker.properties
sed -i "/advertised.listeners=/c\advertised.listeners=PLAINTEXT://broker-0:9092" /kafka/config/broker.properties

# start kafka broker as a background job
/kafka/bin/kafka-server-start.sh /kafka/config/broker.properties &
BROKER_PID=$!

# create topics
/kafka/bin/kafka-topics.sh --create --topic scraping-data --partitions 1 \
    --replication-factor 1 --bootstrap-server localhost:9092

/kafka/bin/kafka-topics.sh --create --topic detection-data --partitions 1 \
    --replication-factor 1 --bootstrap-server localhost:9092

/kafka/bin/kafka-topics.sh --create --topic detection-queries --partitions 1 \
    --replication-factor 1 --bootstrap-server localhost:9092

/kafka/bin/kafka-topics.sh --create --topic detection-responses --partitions 1 \
    --replication-factor 1 --bootstrap-server localhost:9092

wait $BROKER_PID