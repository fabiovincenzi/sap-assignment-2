# Build stage: the whole Gradle multi-project is copied, since drone-service
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :drone-service:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /project/drone-service/build/install/drone-service /app
EXPOSE 8092
CMD ["/app/bin/drone-service"]
