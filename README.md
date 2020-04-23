# Detectify Challenge
## What is it?
A scraper and analytics system to identify technologies used by a list of hosts.
The following diagram describes the entire architecture of the solution:
![Architecture](https://user-images.githubusercontent.com/4671707/80138784-a4d8ef80-85a5-11ea-8a94-0fad4a779a6e.png)

## Usage
From the project root, run:
`$ make start`
to start the system in the background.

## Stack
- Kafka (Messaging/Streaming and Dataflow)
- Git (Versioning)
- Jenkins (Continuous Delivery)
- Docker (Containerization, testing)
- Swagger (API Documentation)

Kafka was selected as stream processing solution as web analytics is its original use case.

## Tests
The testing protocol leverages either Nginx or Docker to simulate a large number of different hosts.

## Scalability
