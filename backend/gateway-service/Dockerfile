FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY ../pom.xml ./pom.xml
COPY ../shared-lib ./shared-lib
COPY . ./gateway-service
RUN cd /app && mvn clean package -pl gateway-service -am -DskipTests -q || \
    (cd /app/gateway-service && mvn clean package -DskipTests -q)

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /app/gateway-service/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
