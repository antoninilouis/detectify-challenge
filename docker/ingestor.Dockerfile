FROM openjdk:15-alpine

RUN apk update && apk upgrade && apk add bash

# download Kafka
RUN wget http://apache.mirrors.spacedump.net/kafka/2.5.0/kafka_2.12-2.5.0.tgz
RUN tar -xzf kafka_2.12-2.5.0.tgz && mv kafka_2.12-2.5.0 kafka

RUN mkdir -p /usr/local/share/kafka/plugins/kafka-connect-jdbc

# download JDBC connector
RUN wget https://packages.confluent.io/maven/io/confluent/kafka-connect-jdbc/5.4.1/kafka-connect-jdbc-5.4.1.jar
RUN mv kafka-connect-jdbc-5.4.1.jar /usr/local/share/kafka/plugins/kafka-connect-jdbc

# download PostgreSQL driver
RUN wget https://repo1.maven.org/maven2/org/postgresql/postgresql/42.2.12/postgresql-42.2.12.jar
RUN mv postgresql-42.2.12.jar /usr/local/share/kafka/plugins/kafka-connect-jdbc

COPY kafka/* /kafka/
