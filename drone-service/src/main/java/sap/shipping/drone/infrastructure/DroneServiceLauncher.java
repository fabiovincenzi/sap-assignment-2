package sap.shipping.drone.infrastructure;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.drone.application.DroneService;
;

public class DroneServiceLauncher extends AbstractVerticle {

    private static final int PORT = 8092;
    private static final String DELIVERY_HOST = "localhost";
    private static final int DELIVERY_PORT = 8091;

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
