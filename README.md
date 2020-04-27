# Detectify Challenge
## A distributed technology scanner
Detectify-challenge scaer is a scraper and an analytics system to identify technologies used by a hosts.
The following diagram describes the solution architecture:
![Architecture](https://user-images.githubusercontent.com/4671707/80138784-a4d8ef80-85a5-11ea-8a94-0fad4a779a6e.png)
\* The diagram omits messaging conversation between the detection API for simplicity

## Usage
From the project root, run:

`$ make build-all`

to build the Docker images from sources. Then,

`$ make start`

to start the system in the background. After the start, the host port "80" is mapped to the service port and requests can be sent.
The OpenAPI standard documentation is available in a Swagger interface served at **/apidocs**

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
The testing protocol leverages Docker to simulate hosts.

## Scaling
