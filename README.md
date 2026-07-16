# Shipping on the Air

Drone-based package delivery system composed of 3 microservices.

## Build

```
./gradlew installDist
```

## Run

Open 3 terminals and execute:

```
./order-service/build/install/order-service/bin/order-service
./delivery-service/build/install/delivery-service/bin/delivery-service
./drone-service/build/install/drone-service/bin/drone-service
```

Ports: Order 8090, Delivery 8091, Drone 8092.

## API Documentation (Swagger UI)

Each service exposes its OpenAPI spec and an interactive Swagger UI:

| Service          | Swagger UI                           | OpenAPI Spec                           |
|------------------|--------------------------------------|----------------------------------------|
| Order Service    | http://localhost:8090/swagger-ui     | http://localhost:8090/api/openapi.yaml |
| Delivery Service | http://localhost:8091/swagger-ui     | http://localhost:8091/api/openapi.yaml |
| Drone Service    | http://localhost:8092/swagger-ui     | http://localhost:8092/api/openapi.yaml |

## Test

```
./gradlew test
```

Runs ArchUnit fitness functions and Cucumber BDD acceptance tests.
