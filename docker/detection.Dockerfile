FROM jazzdd/alpine-flask
RUN apk update && apk upgrade && apk add postgresql-dev gcc python-dev musl-dev
