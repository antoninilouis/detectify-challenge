build-ingestor:
	docker build -f docker/ingestor.Dockerfile -t detectify-challenge/ingestor .

start:
	docker-compose -f docker/compose.yml up -d