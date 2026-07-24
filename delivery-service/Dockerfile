# Build stage: the whole Gradle multi-project is copied, since delivery-service
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :delivery-service:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
# curl is needed by the Docker healthcheck defined in docker-compose.yml
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /project/delivery-service/build/install/delivery-service /app
EXPOSE 8091
EXPOSE 9491
CMD ["/app/bin/delivery-service"]
