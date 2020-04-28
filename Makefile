.PHONY: do_script

build-ingestor:
	docker build -f docker/ingestor.Dockerfile -t detectify-challenge/ingestor .

build-scraper:
	gradle -b scraper/build.gradle distTar
	docker build -f docker/scraper.Dockerfile -t detectify-challenge/scraper .

build-detector:
	gradle -b detector/build.gradle distTar
	docker build -f docker/detector.Dockerfile -t detectify-challenge/detector .

build-detection:
	docker build -f docker/detection.Dockerfile -t detectify-challenge/detection .

stop-all:
	docker rm -f `docker ps -aq`

clear-schemas:
	docker exec `docker ps --filter "name=docker_schema-registry_1" -q` sh -c "curl -X DELETE http://schema-registry:8081/subjects/detection-data-value"
	docker exec `docker ps --filter "name=docker_schema-registry_1" -q` sh -c "curl -X DELETE http://schema-registry:8081/subjects/scraping-data-value"

do_script:
	./download-avro.sh || true

avro: do_script
	rm -rf scraper/src/main/java/types detector/src/main/java/types
	java -jar avro-tools-1.9.2.jar compile schema scraper/src/main/resources/avro/ScraperReport.avsc scraper/src/main/java
	java -jar avro-tools-1.9.2.jar compile schema scraper/src/main/resources/avro/HttpResponseDigest.avsc scraper/src/main/java
	cp -r scraper/src/main/java/types detector/src/main/java
	java -jar avro-tools-1.9.2.jar compile schema detector/src/main/resources/avro/HttpServer.avsc detector/src/main/java
	java -jar avro-tools-1.9.2.jar compile schema detector/src/main/resources/avro/ServerScan.avsc detector/src/main/java

build-all: avro build-ingestor build-scraper build-detector build-detection

rm-apps: clear-schemas
	docker rm -f `docker ps --filter "name=docker_scraper_1" --filter "name=docker_detector_1" --filter "name=docker_detection-service_1" -aq`

start-apps:
	docker-compose -f docker/compose.yml --env-file docker/.dev.env up -d scraper detector detection-service

start:
	docker-compose -f docker/compose.yml --env-file docker/.dev.env up -d
	@echo Waiting for connect worker to be available...
	@sleep 120

	@curl --header "Content-Type: application/json" \
		--request POST --data @kafka/http-server-connector.json \
		http://localhost:8083/connectors

	@curl --header "Content-Type: application/json" \
		--request POST --data @kafka/server-scan-connector.json \
		http://localhost:8083/connectors
