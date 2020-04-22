FROM openjdk:15-alpine
ADD scraper/build/distributions /scraper-distributions
RUN tar -xf /scraper-distributions/scraper.tar
CMD [ "sh", "-c", "sleep 20 && /scraper/bin/scraper" ]
