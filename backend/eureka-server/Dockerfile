FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY shared-lib/pom.xml shared-lib/
COPY eureka-server/pom.xml eureka-server/
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B -pl eureka-server -am 2>/dev/null || true
COPY shared-lib/src shared-lib/src
COPY eureka-server/src eureka-server/src
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl eureka-server -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
