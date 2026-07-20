package sap.shipping.drone.infrastructure;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.drone.application.DroneService;
;

public class DroneServiceLauncher extends AbstractVerticle {

    private static final int PORT = 8092;

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
        var repo = new InMemoryDroneRepository();
        var deliveryProxy = new DeliveryServiceProxy(DELIVERY_HOST, DELIVERY_PORT);

        var service = new DroneService(repo, deliveryProxy);
        var controller = new DroneController(service);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> System.out.println("Drone Service started on port " + PORT));
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new DroneServiceLauncher());
    }
}
