FROM eclipse-temurin:17-jdk-alpine

RUN apk add --no-cache bash

WORKDIR /app

# Copy the wait script and jar
COPY wait-for-it.sh .
COPY target/taskmanager-0.0.1-SNAPSHOT.jar app.jar

RUN chmod +x wait-for-it.sh

EXPOSE 8080

CMD ["bash", "-c", "./wait-for-it.sh db:5432 -- ./wait-for-it.sh redis:6379 -- java -jar app.jar"]
