package delivery_service_tests.steps;

import io.cucumber.java.en.*;
import sap.shipping.delivery.application.DeliveryService;
import sap.shipping.delivery.application.DroneServicePort;
import sap.shipping.delivery.application.OrderServicePort;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

public class DeliverySteps {

    private DeliveryService deliveryService;
    private Delivery currentDelivery;

    public DeliverySteps() {
        var repo = new InMemoryDeliveryRepository();
        DroneServicePort dronePort = new DroneServicePort() {
            @Override
            public Optional<String> requestAvailableDrone(double lat, double lng, double weightKg) {
                return Optional.of("drone-1");
            }
            @Override
            public void releaseDrone(String droneId) {}
        };
        OrderServicePort orderPort = orderId -> {};
        deliveryService = new DeliveryService(repo, dronePort, orderPort);
    }

    @When("a delivery is scheduled for order {string} from \\({double}, {double}) to \\({double}, {double}) weighing {double} kg")
    public void schedule_delivery(String orderId, double pLat, double pLng, double dLat, double dLng, double weight) {
        currentDelivery = deliveryService.scheduleDelivery(orderId, pLat, pLng, dLat, dLng, weight);
    }

    @Then("the delivery is created with status {string}")
    public void delivery_created_with_status(String status) {
        assertThat(currentDelivery.status().name()).isEqualTo(status);
    }

    @Given("a delivery in transit for order {string}")
    public void delivery_in_transit(String orderId) {
        currentDelivery = deliveryService.scheduleDelivery(orderId, 44.0, 12.0, 44.1, 12.1, 2.0);
        currentDelivery = deliveryService.startDelivery(currentDelivery.getId());
    }

    @When("the delivery is completed")
    public void the_delivery_is_completed() {
        currentDelivery = deliveryService.completeDelivery(currentDelivery.getId());
    }

    @Then("the delivery status is {string}")
    public void delivery_status_is(String status) {
        assertThat(currentDelivery.status().name()).isEqualTo(status);
    }
}
