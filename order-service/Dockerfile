# syntax=docker/dockerfile:1
# Build context is the repository root:  docker build -f order-service/Dockerfile .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
COPY order-service/src order-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl order-service -am -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 app
USER app
WORKDIR /app
COPY --from=build /src/order-service/target/order-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
