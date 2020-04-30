<p align="center">
  <img src="https://user-images.githubusercontent.com/4671707/80693933-13510c80-8ad4-11ea-96fa-cb617adc09d1.png">
</p>

# Detectify Challenge
## A distributed technology scanner
Detectify-challenge scaer is a scraper and an analytics system to identify technologies used by a hosts.
The following diagram describes the solution architecture:
![Architecture](https://user-images.githubusercontent.com/4671707/80138784-a4d8ef80-85a5-11ea-8a94-0fad4a779a6e.png)
\* The diagram omits messaging conversation between the detection API for simplicity

## Usage
```shell
# From the project root, run:
$ make build-all

# to build the Docker images from sources. Then,
$ make start
```

to start the system in the background. After the start, the host port "80" is mapped to the service port and requests can be sent.
The OpenAPI standard documentation is available in a Swagger interface served at **/apidocs**. It is possible to try the service from the Swagger interface.

## Stack
- Kafka (Messaging/Streaming and Dataflow). Kafka was selected as stream processing solution as web analytics is its original use case.
- Java HTTP components
- Flask python REST framework
- Docker (Containerization, testing)
- Git (Versioning)
- Swagger (API Documentation)

## Sources \ Project structure
The project source are divided into 3 application directories (detection-service/, detector/, scraper/) and 3 configuration directories (kafka/, docker/, sql/).

## Tests
A Nginx host is added to the Docker bridge network and resolvable as "nginx" for testing. In principle, the scanning system deployed in Docker images can also connect to hosts on the internet.

## Scaling
The system has a double scaling scheme. Kafka is best at scaling horizontally so the number of brokers and Kafka Connect workers is a quick way to scale the messaging.

Most of the topics should be divided in a specific messaging traffic group associated to a consumer or producer group. By associating Kafka topics to specific brokers, the messages from a specific app are handled by a precise group of brokers, making it possible to scale an app messaging horizontally from multiplying the brokers.

For example, the detector engine messaging can be scaled by adding 2 brokers in charge of the detection-data and detection-replies topics.

[AVRO serialization system - https://avro.apache.org/docs/1.8.1/index.html](https://avro.apache.org/docs/1.8.1/index.html)
