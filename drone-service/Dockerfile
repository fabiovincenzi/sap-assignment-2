# Build stage: the whole Gradle multi-project is copied, since drone-service
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :drone-service:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
# curl is needed by the Docker healthcheck defined in docker-compose.yml
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /project/drone-service/build/install/drone-service /app
EXPOSE 8092
EXPOSE 9493
CMD ["/app/bin/drone-service"]
