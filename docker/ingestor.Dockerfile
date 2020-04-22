FROM openjdk:15-alpine
RUN apk update && apk upgrade && apk add bash
RUN wget http://apache.mirrors.spacedump.net/kafka/2.5.0/kafka_2.12-2.5.0.tgz
RUN tar -xzf kafka_2.12-2.5.0.tgz && mv kafka_2.12-2.5.0 kafka
COPY kafka/start-broker.sh /kafka