package sap.shipping.order.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.order.application.OrderService;
import sap.shipping.order.application.UserService;
;

public class OrderServiceLauncher extends AbstractVerticle {

    static Logger logger = Logger.getLogger("[Order Service]");

    private static final int PORT = 8090;

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
        logger.log(Level.INFO, "Order Service initializing...");
        var orderRepo = new InMemoryOrderRepository();
        var userRepo = new InMemoryUserRepository();
        var deliveryProxy = new DeliveryServiceProxy(DELIVERY_HOST, DELIVERY_PORT);

        var orderService = new OrderService(orderRepo, deliveryProxy);
        var userService = new UserService(userRepo);
        var controller = new OrderController(orderService, userService);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> logger.log(Level.INFO, "Order Service ready - port: " + PORT));
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new OrderServiceLauncher());
    }
}
