package sap.shipping.delivery.infrastructure;

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
import sap.shipping.delivery.application.DeliveryService;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;

@Adapter
public class DeliveryController {

    static Logger logger = Logger.getLogger("[Delivery Service Controller]");

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        logger.setLevel(Level.INFO);
        this.deliveryService = deliveryService;
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

        router.post("/api/deliveries").handler(this::scheduleDelivery);
        router.post("/api/deliveries/drone-position").handler(this::updateDronePosition);
        router.post("/api/deliveries/:id/start").handler(this::startDelivery);
        router.post("/api/deliveries/:id/complete").handler(this::completeDelivery);
        router.get("/api/deliveries/:id").handler(this::trackDelivery);
        router.get("/api/deliveries/order/:orderId").handler(this::findByOrderId);

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

    private void scheduleDelivery(RoutingContext ctx) {
        logger.log(Level.INFO, "ScheduleDelivery request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        logger.log(Level.INFO, "Payload: " + body);
        try {
            var delivery = deliveryService.scheduleDelivery(
                body.getString("orderId"),
                body.getDouble("pickupLat"),
                body.getDouble("pickupLng"),
                body.getDouble("deliveryLat"),
                body.getDouble("deliveryLng"),
                body.getDouble("weightKg"));
            ctx.response().setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(deliveryToJson(delivery).encode());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void updateDronePosition(RoutingContext ctx) {
        logger.log(Level.INFO, "UpdateDronePosition request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        try {
            deliveryService.updateDronePosition(
                body.getString("droneId"),
                body.getDouble("lat"),
                body.getDouble("lng"));
            ctx.response().setStatusCode(200).end();
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void startDelivery(RoutingContext ctx) {
        logger.log(Level.INFO, "StartDelivery request - delivery: " + ctx.pathParam("id"));
        try {
            var delivery = deliveryService.startDelivery(new DeliveryId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(deliveryToJson(delivery).encode());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void completeDelivery(RoutingContext ctx) {
        logger.log(Level.INFO, "CompleteDelivery request - delivery: " + ctx.pathParam("id"));
        try {
            var delivery = deliveryService.completeDelivery(new DeliveryId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(deliveryToJson(delivery).encode());
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void trackDelivery(RoutingContext ctx) {
        logger.log(Level.INFO, "TrackDelivery request - delivery: " + ctx.pathParam("id"));
        var delivery = deliveryService.trackDelivery(new DeliveryId(ctx.pathParam("id")));
        if (delivery.isPresent()) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(deliveryToJson(delivery.get()).encode());
        } else {
            ctx.response().setStatusCode(404).end();
        }
    }

    private void findByOrderId(RoutingContext ctx) {
        logger.log(Level.INFO, "FindByOrderId request - order: " + ctx.pathParam("orderId"));
        var delivery = deliveryService.findByOrderId(ctx.pathParam("orderId"));
        if (delivery.isPresent()) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(deliveryToJson(delivery.get()).encode());
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
            <title>Delivery Service API</title>
            <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head><body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
            <script>SwaggerUIBundle({url:'/api/openapi.yaml',dom_id:'#swagger-ui'});</script>
            </body></html>
            """;

    private JsonObject deliveryToJson(Delivery d) {
        return new JsonObject()
            .put("id", d.getId().value())
            .put("orderId", d.orderId())
            .put("status", d.status().name())
            .put("droneId", d.droneId())
            .put("currentLat", d.currentLat())
            .put("currentLng", d.currentLng())
            .put("estimatedMinutes", d.estimatedMinutes())
            .put("createdAt", d.createdAt().toString());
    }
}
