#!/usr/bin/env bash

# configure kafka broker
cp /kafka/config/server.properties /kafka/config/broker.properties
sed -i "/^zookeeper.connect=/c\zookeeper.connect=${ZOOKEEPER_HOST}:2181" /kafka/config/broker.properties

# start kafka broker as a background job
/kafka/bin/kafka-server-start.sh /kafka/config/broker.properties &
BROKER_PID=$!

# create topic and partition
/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic scraping-data --partitions 1 \
    --replication-factor 1

wait $BROKER_PID