build-ingestor:
	docker build -f docker/ingestor.Dockerfile -t detectify-challenge/ingestor .

build-scraper:
	gradle -b scraper/build.gradle distTar
	docker build -f docker/scraper.Dockerfile -t detectify-challenge/scraper .

start:
	docker-compose -f docker/compose.yml up -d