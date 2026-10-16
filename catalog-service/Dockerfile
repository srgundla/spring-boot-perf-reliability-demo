# syntax=docker/dockerfile:1
# Build context is the repository root:  docker build -f catalog-service/Dockerfile .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
# Copy poms first so dependency downloads are cached between code changes
COPY pom.xml .
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
COPY catalog-service/src catalog-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl catalog-service -am -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 app
USER app
WORKDIR /app
COPY --from=build /src/catalog-service/target/catalog-service.jar app.jar
EXPOSE 8081
# Container-aware heap sizing: use 70% of whatever memory limit the container has
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
