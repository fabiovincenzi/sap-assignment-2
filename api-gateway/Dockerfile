# Build stage: the whole Gradle multi-project is copied, since api-gateway
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :api-gateway:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
# curl is needed by the Docker healthcheck defined in docker-compose.yml
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /project/api-gateway/build/install/api-gateway /app
EXPOSE 8080
EXPOSE 9492
CMD ["/app/bin/api-gateway"]
