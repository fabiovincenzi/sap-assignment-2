package sap.shipping.delivery.infrastructure;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.delivery.application.DeliveryService;
;

public class DeliveryServiceLauncher extends AbstractVerticle {

    private static final int PORT = 8091;

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
        var repo = new InMemoryDeliveryRepository();
        var droneProxy = new DroneServiceProxy(DRONE_HOST, DRONE_PORT);
        var orderProxy = new OrderServiceProxy(ORDER_HOST, ORDER_PORT);

        var service = new DeliveryService(repo, droneProxy, orderProxy);
        var controller = new DeliveryController(service);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> System.out.println("Delivery Service started on port " + PORT));
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new DeliveryServiceLauncher());
    }
}
