# Build stage: the whole Gradle multi-project is copied, since api-gateway
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :api-gateway:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /project/api-gateway/build/install/api-gateway /app
EXPOSE 8080
CMD ["/app/bin/api-gateway"]
