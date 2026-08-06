package sap.shipping.gateway.infrastructure;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.gateway.application.DeliveryServicePort;
import sap.shipping.gateway.application.DroneServicePort;
import sap.shipping.gateway.application.OrderServicePort;
import sap.shipping.gateway.application.ServiceNotAvailableException;
import sap.shipping.gateway.domain.DroneView;
import sap.shipping.gateway.domain.NewOrder;
import sap.shipping.gateway.domain.OrderView;
import sap.shipping.gateway.domain.TrackingInfo;
import sap.shipping.gateway.domain.UserView;

/**
 * Inbound adapter of the API Gateway. It exposes a single, versioned public API
 * (/api/v1/...) and routes each request to the appropriate service through the
 * outbound ports (proxies).
 *
 * The proxies are synchronous, so every handler delegates the call to a worker
 * thread via vertx.executeBlocking(...) with ordered=false, to keep the event
 * loop free.
 */
@Adapter
public class GatewayController {

    static Logger logger = Logger.getLogger("[API Gateway Controller]");

    private static final String API = "/api/v1";

    private final OrderServicePort orderService;
    private final DeliveryServicePort deliveryService;
    private final DroneServicePort droneService;
    private final GatewayMetrics metrics;

    public GatewayController(OrderServicePort orderService,
                             DeliveryServicePort deliveryService,
                             DroneServicePort droneService,
                             GatewayMetrics metrics) {
        logger.setLevel(Level.INFO);
        this.orderService = orderService;
        this.deliveryService = deliveryService;
        this.droneService = droneService;
        this.metrics = metrics;
    }

    public Router createRouter(Vertx vertx) {
        var router = Router.router(vertx);
        if (metrics != null) {
            router.route().handler(ctx -> {
                metrics.incRequest();
                ctx.next();
            });
        }
        router.route().handler(BodyHandler.create());
        router.route().failureHandler(ctx -> {
            var error = ctx.failure();
            ctx.response().setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("error", error != null ? error.getMessage() : "Unknown error").encode());
        });

        // Users (proxied to Order Service)
        router.post(API + "/users/register").handler(this::register);
        router.post(API + "/users/login").handler(this::login);

        // Orders (proxied to Order Service)
        router.post(API + "/orders").handler(this::createOrder);
        router.get(API + "/orders/:id").handler(this::getOrder);
        router.post(API + "/orders/:id/confirm").handler(this::confirmOrder);
        router.post(API + "/orders/:id/cancel").handler(this::cancelOrder);

        // Fleet (proxied to Drone Service)
        router.post(API + "/drones").handler(this::registerDrone);

        // Tracking (aggregation of Order + Delivery)
        router.get(API + "/tracking/:orderId").handler(this::getTracking);

        // Health Check API (infrastructure endpoint, unversioned)
        router.get("/health").handler(this::healthCheck);

        return router;
    }

    /**
     * Health Check API (observability pattern): reports whether the gateway is
     * able to handle requests. It does not probe the downstream services, whose
     * health is reported by their own endpoint.
     */
    private void healthCheck(RoutingContext ctx) {
        var reply = new JsonObject()
            .put("status", "UP")
            .put("checks", new JsonArray());
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(reply.encode());
    }

    /* ---------- Users ---------- */

    private void register(RoutingContext ctx) {
        logger.log(Level.INFO, "Register request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        ctx.vertx().executeBlocking(
                () -> orderService.register(body.getString("username"), body.getString("password")), false)
            .onSuccess(user -> sendJson(ctx, 201, userToJson(user)))
            .onFailure(err -> sendError(ctx, err));
    }

    private void login(RoutingContext ctx) {
        logger.log(Level.INFO, "Login request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        ctx.vertx().executeBlocking(
                () -> orderService.login(body.getString("username"), body.getString("password")), false)
            .onSuccess(user -> sendJson(ctx, 200, userToJson(user)))
            .onFailure(err -> sendError(ctx, err));
    }

    /* ---------- Orders ---------- */

    private void createOrder(RoutingContext ctx) {
        logger.log(Level.INFO, "CreateOrder request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        var newOrder = new NewOrder(
            body.getString("customerId"),
            body.getString("pickupStreet"), body.getDouble("pickupLat"), body.getDouble("pickupLng"),
            body.getString("deliveryStreet"), body.getDouble("deliveryLat"), body.getDouble("deliveryLng"),
            body.getDouble("weightKg"));
        ctx.vertx().executeBlocking(() -> orderService.createOrder(newOrder), false)
            .onSuccess(order -> sendJson(ctx, 201, orderToJson(order)))
            .onFailure(err -> sendError(ctx, err));
    }

    private void getOrder(RoutingContext ctx) {
        var id = ctx.pathParam("id");
        logger.log(Level.INFO, "GetOrder request - order: " + id);
        ctx.vertx().<Optional<OrderView>>executeBlocking(() -> orderService.getOrder(id), false)
            .onSuccess(opt -> {
                if (opt.isPresent()) {
                    sendJson(ctx, 200, orderToJson(opt.get()));
                } else {
                    ctx.response().setStatusCode(404)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject().put("error", "order not found").encode());
                }
            })
            .onFailure(err -> sendError(ctx, err));
    }

    private void confirmOrder(RoutingContext ctx) {
        var id = ctx.pathParam("id");
        logger.log(Level.INFO, "ConfirmOrder request - order: " + id);
        ctx.vertx().executeBlocking(() -> orderService.confirmOrder(id), false)
            .onSuccess(order -> sendJson(ctx, 200, orderToJson(order)))
            .onFailure(err -> sendError(ctx, err));
    }

    private void cancelOrder(RoutingContext ctx) {
        var id = ctx.pathParam("id");
        logger.log(Level.INFO, "CancelOrder request - order: " + id);
        ctx.vertx().executeBlocking(() -> orderService.cancelOrder(id), false)
            .onSuccess(order -> sendJson(ctx, 200, orderToJson(order)))
            .onFailure(err -> sendError(ctx, err));
    }

    /* ---------- Fleet ---------- */

    private void registerDrone(RoutingContext ctx) {
        logger.log(Level.INFO, "RegisterDrone request - " + ctx.currentRoute().getPath());
        var body = ctx.body().asJsonObject();
        ctx.vertx().executeBlocking(() -> droneService.registerDrone(
                body.getDouble("maxWeightKg"), body.getDouble("lat"), body.getDouble("lng")), false)
            .onSuccess(drone -> sendJson(ctx, 201, droneToJson(drone)))
            .onFailure(err -> sendError(ctx, err));
    }

    /* ---------- Tracking (aggregation) ---------- */

    private void getTracking(RoutingContext ctx) {
        var orderId = ctx.pathParam("orderId");
        logger.log(Level.INFO, "GetTracking request - order: " + orderId);
        // API Composition: the gateway queries two services (Order + Delivery)
        // and joins their responses into a single TrackingInfo for the client.
        ctx.vertx().<TrackingInfo>executeBlocking(() -> {
            var orderOpt = orderService.getOrder(orderId);
            if (orderOpt.isEmpty()) {
                return null; // signals "order not found" -> handled as 404 below
            }
            var order = orderOpt.get();
            var deliveryOpt = deliveryService.findByOrderId(orderId);
            if (deliveryOpt.isPresent()) {
                var d = deliveryOpt.get();
                return new TrackingInfo(orderId, order.status(), true,
                    d.status(), d.droneId(), d.currentLat(), d.currentLng(), d.estimatedMinutes());
            }
            return TrackingInfo.orderOnly(orderId, order.status());
        }, false)
            .onSuccess(tracking -> {
                if (tracking == null) {
                    ctx.response().setStatusCode(404)
                        .putHeader("Content-Type", "application/json")
                        .end(new JsonObject().put("error", "order not found").encode());
                } else {
                    sendJson(ctx, 200, trackingToJson(tracking));
                }
            })
            .onFailure(err -> sendError(ctx, err));
    }

    /* ---------- Helpers ---------- */

    private void sendJson(RoutingContext ctx, int status, JsonObject body) {
        ctx.response().setStatusCode(status)
            .putHeader("Content-Type", "application/json")
            .end(body.encode());
    }

    /**
     * Any inter-service failure surfaces as ServiceNotAvailableException and is
     * translated into a single, uniform 502 Bad Gateway for the client.
     */
    private void sendError(RoutingContext ctx, Throwable err) {
        logger.log(Level.WARNING, "Downstream failure: " + err.getMessage());
        int status = (err instanceof ServiceNotAvailableException) ? 502 : 500;
        ctx.response().setStatusCode(status)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("error", err.getMessage()).encode());
    }

    private JsonObject userToJson(UserView u) {
        return new JsonObject()
            .put("userId", u.userId())
            .put("username", u.username());
    }

    private JsonObject orderToJson(OrderView o) {
        return new JsonObject()
            .put("id", o.id())
            .put("customerId", o.customerId())
            .put("status", o.status())
            .put("pickupStreet", o.pickupStreet())
            .put("deliveryStreet", o.deliveryStreet())
            .put("weightKg", o.weightKg())
            .put("createdAt", o.createdAt());
    }

    private JsonObject droneToJson(DroneView d) {
        return new JsonObject()
            .put("id", d.id())
            .put("maxWeightKg", d.maxWeightKg())
            .put("lat", d.lat())
            .put("lng", d.lng())
            .put("status", d.status());
    }

    private JsonObject trackingToJson(TrackingInfo t) {
        var json = new JsonObject()
            .put("orderId", t.orderId())
            .put("orderStatus", t.orderStatus())
            .put("hasDelivery", t.hasDelivery());
        if (t.hasDelivery()) {
            json.put("deliveryStatus", t.deliveryStatus())
                .put("droneId", t.droneId())
                .put("currentLat", t.currentLat())
                .put("currentLng", t.currentLng())
                .put("estimatedMinutes", t.estimatedMinutes());
        }
        return json;
    }
}
