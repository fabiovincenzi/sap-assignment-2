package sap.shipping.order.infrastructure;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import sap.shipping.order.application.OrderService;
import sap.shipping.order.application.UserService;
;

public class OrderServiceLauncher extends AbstractVerticle {

    private static final int PORT = 8090;
    private static final String DELIVERY_HOST = "localhost";
    private static final int DELIVERY_PORT = 8091;

    @Override
    public void start() {
        var orderRepo = new InMemoryOrderRepository();
        var userRepo = new InMemoryUserRepository();
        var deliveryProxy = new DeliveryServiceProxy(DELIVERY_HOST, DELIVERY_PORT);

        var orderService = new OrderService(orderRepo, deliveryProxy);
        var userService = new UserService(userRepo);
        var controller = new OrderController(orderService, userService);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(PORT)
            .onSuccess(s -> System.out.println("Order Service started on port " + PORT));
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new OrderServiceLauncher());
    }
}
