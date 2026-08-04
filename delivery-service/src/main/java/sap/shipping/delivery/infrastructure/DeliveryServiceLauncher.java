package sap.shipping.delivery.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.delivery.application.DeliveryServiceImpl;
;

public class DeliveryServiceLauncher extends AbstractVerticle {

    static Logger logger = Logger.getLogger("[Delivery Service]");

    private static final int PORT = 8091;
    private static final int METRICS_PORT = 9491;

    /* Externalized configuration: defaults target a manual local deployment,
       the docker-compose file overrides them with the service container names. */
    private static final String DRONE_HOST = env("DRONE_HOST", "localhost");
    private static final int DRONE_PORT = Integer.parseInt(env("DRONE_PORT", "8092"));
    private static final String ORDER_HOST = env("ORDER_HOST", "localhost");
    private static final int ORDER_PORT = Integer.parseInt(env("ORDER_PORT", "8090"));

    private static String env(String name, String defaultValue) {
        var value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "Delivery Service initializing...");
        var repo = new EventSourcedDeliveryRepository(new InMemoryDeliveryEventStore(), new InMemoryDeliverySnapshotStore(), new InMemoryDeliveryLookupView());
        var droneProxy = new DroneServiceProxy(DRONE_HOST, DRONE_PORT);
        var orderProxy = new OrderServiceProxy(ORDER_HOST, ORDER_PORT);

        var service = new DeliveryServiceImpl(repo, droneProxy, orderProxy);
        var controller = new DeliveryController(service);

        try {
            service.addObserver(new PrometheusDeliveryServiceObserver(METRICS_PORT));
            logger.log(Level.INFO, "Prometheus metrics server ready - port: " + METRICS_PORT);
        } catch (ObsMetricServerException e) {
            logger.log(Level.SEVERE, "Failed to start Prometheus metrics server - " + e.getMessage());
        }

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> logger.log(Level.INFO, "Delivery Service ready - port: " + PORT))
            .onFailure(err -> {
                logger.log(Level.SEVERE, "Delivery Service failed to bind port " + PORT + " - " + err.getMessage());
                vertx.close().onComplete(v -> System.exit(1));
            });
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new DeliveryServiceLauncher());
    }
}
