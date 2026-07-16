package order_service_tests.steps;

import io.cucumber.java.en.*;
import sap.shipping.order.application.OrderService;
import sap.shipping.order.application.UserService;
import sap.shipping.order.domain.*;
import sap.shipping.order.domain.events.OrderConfirmed;
import sap.shipping.order.infrastructure.InMemoryOrderRepository;
import sap.shipping.order.infrastructure.InMemoryUserRepository;

import static org.assertj.core.api.Assertions.*;

public class OrderSteps {

    private OrderService orderService;
    private UserService userService;
    private Order currentOrder;

    public OrderSteps() {
        var orderRepo = new InMemoryOrderRepository();
        var userRepo = new InMemoryUserRepository();
        orderService = new OrderService(orderRepo, event -> {});
        userService = new UserService(userRepo);
    }

    @Given("a registered customer {string}")
    public void a_registered_customer(String username) {
        userService.register(username, "password");
    }

    @When("{string} creates an order with pickup {string} and delivery {string} weighing {double} kg")
    public void creates_an_order(String customer, String pickup, String delivery, double weight) {
        currentOrder = orderService.createOrder(
            customer,
            new Address(pickup, 44.0, 12.0),
            new Address(delivery, 44.1, 12.1),
            new PackageInfo(weight)
        );
    }

    @Then("the order is created in PENDING status")
    public void order_is_pending() {
        assertThat(currentOrder.status()).isEqualTo(OrderStatus.PENDING);
    }

    @When("the order is confirmed")
    public void the_order_is_confirmed() {
        currentOrder = orderService.confirmOrder(currentOrder.getId());
    }

    @When("the order is cancelled")
    public void the_order_is_cancelled() {
        currentOrder = orderService.cancelOrder(currentOrder.getId());
    }

    @Then("the order status is {string}")
    public void the_order_status_is(String status) {
        assertThat(currentOrder.status().name()).isEqualTo(status);
    }
}
