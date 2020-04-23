#!/usr/bin/env bash

# configure kafka connect standalone worker
cp /kafka/config/connect-standalone.properties /kafka/config/standalone-worker.properties
sed -i "/^bootstrap.servers=/c\bootstrap.servers=${BROKER_HOST}:${BROKER_PORT}" /kafka/config/standalone-worker.properties
sed -i "/key.converter=/c\key.converter=org.apache.kafka.connect.json.JsonConverter" /kafka/config/standalone-worker.properties
sed -i "/value.converter=/c\value.converter=org.apache.kafka.connect.json.JsonConverter" /kafka/config/standalone-worker.properties
sed -i "/key.converter.schemas.enable=/c\key.converter.schemas.enable=true" /kafka/config/standalone-worker.properties
sed -i "/value.converter.schemas.enable=/c\value.converter.schemas.enable=true" /kafka/config/standalone-worker.properties
sed -i "/plugin.path=/c\plugin.path=/usr/local/share/kafka/plugins" /kafka/config/standalone-worker.properties

# configure kafka connector
sed -i "/connection.user=/c\connection.user=${POSTGRES_USER}" /kafka/postgresql-connector.properties
sed -i "/connection.password=/c\connection.password=${POSTGRES_PASSWORD}" /kafka/postgresql-connector.properties

# start kafka connect standalone worker as a background process
/kafka/bin/connect-standalone.sh /kafka/config/standalone-worker.properties /kafka/postgresql-connector.properties &
WORKER_PID=$!

wait $WORKER_PID