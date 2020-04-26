build-ingestor:
	docker build -f docker/ingestor.Dockerfile -t detectify-challenge/ingestor .

build-scraper:
	gradle -b scraper/build.gradle distTar
	docker build -f docker/scraper.Dockerfile -t detectify-challenge/scraper .

build-detector:
	gradle -b detector/build.gradle distTar
	docker build -f docker/detector.Dockerfile -t detectify-challenge/detector .

build-all: build-ingestor build-scraper build-detector

stop-all:
	docker rm -f `docker ps -aq`

avro:
	rm -rf scraper/src/main/java/types detector/src/main/java/types
	java -jar avro-tools-1.9.2.jar compile schema scraper/src/main/java/avro/HttpResponseDigest.avsc scraper/src/main/java
	cp -r scraper/src/main/java/types detector/src/main/java

start:
	docker-compose -f docker/compose.yml --env-file docker/.dev.env up -d
