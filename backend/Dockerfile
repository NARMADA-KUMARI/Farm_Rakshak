# Unified multi-stage Dockerfile for FarmRakshak Java services
# Usage: docker build --build-arg SERVICE_NAME=auth-service --build-arg SERVICE_PORT=8081 .
ARG SERVICE_NAME

FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
ARG SERVICE_NAME
WORKDIR /app
COPY pom.xml .
COPY shared-lib/pom.xml shared-lib/
COPY eureka-server/pom.xml eureka-server/
COPY gateway-service/pom.xml gateway-service/
COPY auth-service/pom.xml auth-service/
COPY user-service/pom.xml user-service/
COPY crop-service/pom.xml crop-service/
COPY ai-client-service/pom.xml ai-client-service/
COPY weather-service/pom.xml weather-service/
COPY advisory-service/pom.xml advisory-service/
COPY notification-service/pom.xml notification-service/
COPY blog-service/pom.xml blog-service/
COPY admin-service/pom.xml admin-service/
COPY market-data-service/pom.xml market-data-service/
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am -B 2>/dev/null || true
COPY shared-lib/src shared-lib/src
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -q || \
    (sleep 5 && mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -q) || \
    (sleep 10 && mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -q)

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
WORKDIR /app
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", "-jar", "app.jar"]
