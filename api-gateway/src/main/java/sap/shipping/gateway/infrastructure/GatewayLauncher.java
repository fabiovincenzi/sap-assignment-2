package sap.shipping.gateway.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

/**
 * Composition root of the API Gateway: wires the three proxies into the
 * controller and starts the HTTP server on port 8080.
 */
public class GatewayLauncher extends AbstractVerticle {

    static Logger logger = Logger.getLogger("[API Gateway]");

    private static final int PORT = 8080;
    private static final int METRICS_PORT = 9492;

    /* Externalized configuration: defaults target a manual local deployment,
       the docker-compose file overrides them with the service container names. */
    private static final String ORDER_HOST = env("ORDER_HOST", "localhost");
    private static final int ORDER_PORT = Integer.parseInt(env("ORDER_PORT", "8090"));
    private static final String DELIVERY_HOST = env("DELIVERY_HOST", "localhost");
    private static final int DELIVERY_PORT = Integer.parseInt(env("DELIVERY_PORT", "8091"));
    private static final String DRONE_HOST = env("DRONE_HOST", "localhost");
    private static final int DRONE_PORT = Integer.parseInt(env("DRONE_PORT", "8092"));

    private static String env(String name, String defaultValue) {
        var value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "API Gateway initializing...");
        var orderProxy = new OrderServiceProxy(ORDER_HOST, ORDER_PORT);
        var deliveryProxy = new DeliveryServiceProxy(DELIVERY_HOST, DELIVERY_PORT);
        var droneProxy = new DroneServiceProxy(DRONE_HOST, DRONE_PORT);

        GatewayMetrics metrics = null;
        try {
            metrics = new GatewayMetrics(METRICS_PORT);
            logger.log(Level.INFO, "Prometheus metrics server ready - port: " + METRICS_PORT);
        } catch (ObsMetricServerException e) {
            logger.log(Level.SEVERE, "Failed to start Prometheus metrics server - " + e.getMessage());
        }

        var controller = new GatewayController(orderProxy, deliveryProxy, droneProxy, metrics);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> logger.log(Level.INFO, "API Gateway ready - port: " + PORT))
            .onFailure(err -> {
                logger.log(Level.SEVERE, "API Gateway failed to bind port " + PORT + " - " + err.getMessage());
                vertx.close().onComplete(v -> System.exit(1));
            });
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new GatewayLauncher());
    }
}
