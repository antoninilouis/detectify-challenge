FROM openjdk:15-alpine
ADD detector/build/distributions /detector-distributions
RUN tar -xf /detector-distributions/detector.tar
CMD [ "sh", "-c", "sleep 20 && /detector/bin/detector" ]
