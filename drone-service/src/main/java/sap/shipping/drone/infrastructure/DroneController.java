package sap.shipping.drone.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.drone.application.DroneService;
import sap.shipping.drone.domain.Drone;
import sap.shipping.drone.domain.DroneId;

@Adapter
public class DroneController {

    static Logger logger = Logger.getLogger("[Drone Service Controller]");

    private final DroneService droneService;

    public DroneController(DroneService droneService) {
        logger.setLevel(Level.INFO);
        this.droneService = droneService;
    }

    public Router createRouter(Vertx vertx) {
        var router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(ctx -> {
            var error = ctx.failure();
            if (error != null) error.printStackTrace();
            ctx.response().setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", error != null ? error.getMessage() : "Unknown error").encode());
        });

        router.post("/api/drones").handler(this::registerDrone);
        router.get("/api/drones/available").handler(this::findAvailable);
        router.post("/api/drones/:id/assign").handler(this::assignDrone);
        router.post("/api/drones/:id/location").handler(this::updateLocation);
        router.post("/api/drones/:id/release").handler(this::releaseDrone);
        router.get("/api/drones/:id").handler(this::getDrone);

        router.get("/health").handler(this::healthCheck);

        router.get("/api/openapi.yaml").handler(this::serveOpenApi);
        router.get("/swagger-ui").handler(this::serveSwaggerUi);

        return router;
    }

    /**
     * Health Check API (observability pattern). Reports whether the service is
     * able to handle requests. The repository is in-memory, so there is no
     * external dependency to probe.
     */
    private void healthCheck(RoutingContext ctx) {
        var reply = new JsonObject()
            .put("status", "UP")
            .put("checks", new JsonArray());
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(reply.encode());
    }

    private void registerDrone(RoutingContext ctx) {
        logger.log(Level.INFO, "RegisterDrone request - " + ctx.currentRoute().getPath());
        try {
            var body = ctx.body().asJsonObject();
            logger.log(Level.INFO, "Payload: " + body);
            var drone = droneService.registerDrone(
                body.getDouble("maxWeightKg"),
                body.getDouble("lat"),
                body.getDouble("lng"));
            ctx.response().setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(droneToJson(drone).encode());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void findAvailable(RoutingContext ctx) {
        logger.log(Level.INFO, "FindAvailableDrone request - " + ctx.request().query());
        double lat = Double.parseDouble(ctx.queryParam("lat").get(0));
        double lng = Double.parseDouble(ctx.queryParam("lng").get(0));
        double weightKg = Double.parseDouble(ctx.queryParam("weightKg").get(0));

        var drone = droneService.findAvailableDrone(lat, lng, weightKg);
        if (drone.isPresent()) {
            var d = drone.get();
            droneService.assignDrone(d.getId());
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("droneId", d.getId().value()).encode());
        } else {
            ctx.response().setStatusCode(404)
                .end(new JsonObject().put("error", "No available drone").encode());
        }
    }

    private void assignDrone(RoutingContext ctx) {
        logger.log(Level.INFO, "AssignDrone request - drone: " + ctx.pathParam("id"));
        try {
            var drone = droneService.assignDrone(new DroneId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(droneToJson(drone).encode());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void updateLocation(RoutingContext ctx) {
        logger.log(Level.INFO, "UpdateLocation request - drone: " + ctx.pathParam("id"));
        var body = ctx.body().asJsonObject();
        try {
            droneService.updateLocation(
                new DroneId(ctx.pathParam("id")),
                body.getDouble("lat"),
                body.getDouble("lng"));
            ctx.response().setStatusCode(200).end();
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void releaseDrone(RoutingContext ctx) {
        logger.log(Level.INFO, "ReleaseDrone request - drone: " + ctx.pathParam("id"));
        try {
            var drone = droneService.releaseDrone(new DroneId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(droneToJson(drone).encode());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getDrone(RoutingContext ctx) {
        logger.log(Level.INFO, "GetDrone request - drone: " + ctx.pathParam("id"));
        var drone = droneService.getDrone(new DroneId(ctx.pathParam("id")));
        if (drone.isPresent()) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(droneToJson(drone.get()).encode());
        } else {
            ctx.response().setStatusCode(404).end();
        }
    }

    private void serveOpenApi(RoutingContext ctx) {
        try (var is = getClass().getClassLoader().getResourceAsStream("openapi.yaml")) {
            ctx.response()
                .putHeader("Content-Type", "text/yaml")
                .end(new String(is.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            ctx.response().setStatusCode(500).end();
        }
    }

    private void serveSwaggerUi(RoutingContext ctx) {
        ctx.response()
            .putHeader("Content-Type", "text/html")
            .end(SWAGGER_UI_HTML);
    }

    private static final String SWAGGER_UI_HTML = """
            <!DOCTYPE html>
            <html><head>
            <title>Drone Service API</title>
            <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head><body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
            <script>SwaggerUIBundle({url:'/api/openapi.yaml',dom_id:'#swagger-ui'});</script>
            </body></html>
            """;

    private JsonObject droneToJson(Drone d) {
        return new JsonObject()
            .put("id", d.getId().value())
            .put("maxWeightKg", d.maxWeightKg())
            .put("lat", d.location().lat())
            .put("lng", d.location().lng())
            .put("status", d.status().name());
    }
}
