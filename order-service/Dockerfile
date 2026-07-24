# Build stage: the whole Gradle multi-project is copied, since order-service
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :order-service:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
# curl is needed by the Docker healthcheck defined in docker-compose.yml
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /project/order-service/build/install/order-service /app
EXPOSE 8090
EXPOSE 9490
CMD ["/app/bin/order-service"]
