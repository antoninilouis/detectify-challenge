FROM openjdk:15-alpine
ADD scraper/build/distributions /scraper-distributions
RUN tar -xf /scraper-distributions/scraper.tar
CMD [ "/scraper/bin/scraper" ]
