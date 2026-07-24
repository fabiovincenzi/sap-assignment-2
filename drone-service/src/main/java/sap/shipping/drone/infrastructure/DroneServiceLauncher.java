package sap.shipping.drone.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.drone.application.DroneService;
;

public class DroneServiceLauncher extends AbstractVerticle {

    static Logger logger = Logger.getLogger("[Drone Service]");

    private static final int PORT = 8092;
    private static final int METRICS_PORT = 9493;

    /* Externalized configuration: defaults target a manual local deployment,
       the docker-compose file overrides them with the service container names. */
    private static final String DELIVERY_HOST = env("DELIVERY_HOST", "localhost");
    private static final int DELIVERY_PORT = Integer.parseInt(env("DELIVERY_PORT", "8091"));

    private static String env(String name, String defaultValue) {
        var value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "Drone Service initializing...");
        var repo = new InMemoryDroneRepository();
        var deliveryProxy = new DeliveryServiceProxy(DELIVERY_HOST, DELIVERY_PORT);

        var service = new DroneService(repo, deliveryProxy);
        var controller = new DroneController(service);

        try {
            service.addObserver(new PrometheusDroneServiceObserver(METRICS_PORT));
            logger.log(Level.INFO, "Prometheus metrics server ready - port: " + METRICS_PORT);
        } catch (ObsMetricServerException e) {
            logger.log(Level.SEVERE, "Failed to start Prometheus metrics server - " + e.getMessage());
        }

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> logger.log(Level.INFO, "Drone Service ready - port: " + PORT))
            .onFailure(err -> {
                logger.log(Level.SEVERE, "Drone Service failed to bind port " + PORT + " - " + err.getMessage());
                vertx.close().onComplete(v -> System.exit(1));
            });
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new DroneServiceLauncher());
    }
}
