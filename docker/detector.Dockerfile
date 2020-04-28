FROM openjdk:15-alpine
ADD detector/build/distributions /detector-distributions
RUN tar -xf /detector-distributions/detector.tar
CMD [ "/detector/bin/detector" ]
