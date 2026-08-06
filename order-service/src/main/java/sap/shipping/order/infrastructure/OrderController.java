package sap.shipping.order.infrastructure;

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
import sap.shipping.order.application.OrderService;
import sap.shipping.order.application.UserService;
import sap.shipping.order.domain.*;

@Adapter
public class OrderController {

    static Logger logger = Logger.getLogger("[Order Service Controller]");

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        logger.setLevel(Level.INFO);
        this.orderService = orderService;
        this.userService = userService;
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

        router.post("/api/users/register").handler(this::register);
        router.post("/api/users/login").handler(this::login);
        router.post("/api/orders").handler(this::createOrder);
        router.post("/api/orders/:id/confirm").handler(this::confirmOrder);
        router.post("/api/orders/:id/cancel").handler(this::cancelOrder);
        router.get("/api/orders/:id").handler(this::getOrder);
        router.post("/api/orders/:id/complete").handler(this::completeOrder);

        router.get("/health").handler(this::healthCheck);

        router.get("/api/openapi.yaml").handler(this::serveOpenApi);
        router.get("/swagger-ui").handler(this::serveSwaggerUi);

        return router;
    }

    /**
     * Health Check API (observability pattern). Reports whether the service is
     * able to handle requests. The repositories are in-memory, so there is no
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

    private void register(RoutingContext ctx) {
        logger.log(Level.INFO, "Register request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        try {
            var user = userService.register(body.getString("username"), body.getString("password"));
            ctx.response().setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("userId", user.getId().value()).put("username", user.username()).encode());
        } catch (IllegalArgumentException e) {
            ctx.response().setStatusCode(409).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void login(RoutingContext ctx) {
        logger.log(Level.INFO, "Login request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        try {
            var user = userService.login(body.getString("username"), body.getString("password"));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("userId", user.getId().value()).put("username", user.username()).encode());
        } catch (IllegalArgumentException e) {
            ctx.response().setStatusCode(401).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void createOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "CreateOrder request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        logger.log(Level.INFO, "Payload: " + body);
        try {
            var pickup = new Address(body.getString("pickupStreet"), body.getDouble("pickupLat"), body.getDouble("pickupLng"));
            var delivery = new Address(body.getString("deliveryStreet"), body.getDouble("deliveryLat"), body.getDouble("deliveryLng"));
            var packageInfo = new PackageInfo(body.getDouble("weightKg"));
            var order = orderService.createOrder(body.getString("customerId"), pickup, delivery, packageInfo);
            ctx.response().setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(orderToJson(order).encode());
        } catch (IllegalArgumentException e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void confirmOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "ConfirmOrder request - order: " + ctx.pathParam("id"));
        try {
            var order = orderService.confirmOrder(new OrderId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(orderToJson(order).encode());
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void cancelOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "CancelOrder request - order: " + ctx.pathParam("id"));
        try {
            var order = orderService.cancelOrder(new OrderId(ctx.pathParam("id")));
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(orderToJson(order).encode());
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void completeOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "CompleteOrder request - order: " + ctx.pathParam("id"));
        try {
            orderService.completeOrder(new OrderId(ctx.pathParam("id")));
            ctx.response().setStatusCode(200).end();
        } catch (IllegalArgumentException | IllegalStateException e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void getOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "GetOrder request - order: " + ctx.pathParam("id"));
        var order = orderService.getOrder(new OrderId(ctx.pathParam("id")));
        if (order.isPresent()) {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(orderToJson(order.get()).encode());
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
            <title>Order Service API</title>
            <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head><body>
            <div id="swagger-ui"></div>
            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
            <script>SwaggerUIBundle({url:'/api/openapi.yaml',dom_id:'#swagger-ui'});</script>
            </body></html>
            """;

    private JsonObject orderToJson(Order order) {
        return new JsonObject()
            .put("id", order.getId().value())
            .put("customerId", order.customerId())
            .put("pickupStreet", order.pickup().street())
            .put("deliveryStreet", order.delivery().street())
            .put("weightKg", order.packageInfo().weightKg())
            .put("status", order.status().name())
            .put("createdAt", order.createdAt().toString());
    }
}
