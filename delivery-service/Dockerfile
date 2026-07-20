# Build stage: the whole Gradle multi-project is copied, since delivery-service
# depends on the shared :common module.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /project
COPY . /project/
RUN ./gradlew :delivery-service:installDist --no-daemon

# Runtime stage: only the installed distribution is kept.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /project/delivery-service/build/install/delivery-service /app
EXPOSE 8091
CMD ["/app/bin/delivery-service"]
