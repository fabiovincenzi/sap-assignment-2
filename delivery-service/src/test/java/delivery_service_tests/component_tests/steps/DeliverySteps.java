package delivery_service_tests.component_tests.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import delivery_service_tests.infrastructure.Synchroniser;
import sap.shipping.delivery.application.DroneServicePort;
import sap.shipping.delivery.application.OrderServicePort;
import sap.shipping.delivery.application.DeliveryServiceImpl;
import sap.shipping.delivery.infrastructure.DeliveryController;
import sap.shipping.delivery.infrastructure.EventSourcedDeliveryRepository;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryEventStore;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryLookupView;
import sap.shipping.delivery.infrastructure.InMemoryDeliverySnapshotStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Component test: the delivery service is started whole - controller, application and
 * event-sourced persistence - and driven from the outside through its HTTP API, exactly
 * as a client would. Only the other services are stubbed.
 */
public class DeliverySteps {

    private static final int SERVICE_PORT = 9594;
    private static final String DELIVERIES_ENDPOINT = "http://localhost:" + SERVICE_PORT + "/api/deliveries";

    private Vertx vertx;
    private JsonObject currentDelivery;

    @Before
    public void startTheService() throws Exception {
        var sync = new Synchroniser();
        vertx = Vertx.vertx();

        var repository = new EventSourcedDeliveryRepository(new InMemoryDeliveryEventStore(),
            new InMemoryDeliverySnapshotStore(), new InMemoryDeliveryLookupView());

        /* the neighbouring services are stubbed: the subject here is the delivery service alone */
        DroneServicePort dronePort = new DroneServicePort() {
            @Override
            public Optional<String> requestAvailableDrone(double lat, double lng, double weightKg) {
                return Optional.of("drone-1");
            }
            @Override
            public void releaseDrone(String droneId) {}
        };
        OrderServicePort orderPort = orderId -> {};

        var service = new DeliveryServiceImpl(repository, dronePort, orderPort);
        var controller = new DeliveryController(service);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(SERVICE_PORT)
            .onSuccess(server -> sync.notifySync());
        sync.awaitSync();
    }

    @After
    public void stopTheService() {
        vertx.close();
    }

    @When("a delivery is scheduled for order {string} from \\({double}, {double}) to \\({double}, {double}) weighing {double} kg")
    public void schedule_delivery(String orderId, double pLat, double pLng, double dLat, double dLng, double weight) throws Exception {
        var body = new JsonObject()
            .put("orderId", orderId)
            .put("pickupLat", pLat).put("pickupLng", pLng)
            .put("deliveryLat", dLat).put("deliveryLng", dLng)
            .put("weightKg", weight);
        currentDelivery = new JsonObject(post(DELIVERIES_ENDPOINT, body).body());
    }

    @Then("the delivery is created with status {string}")
    public void delivery_created_with_status(String status) {
        assertThat(currentDelivery.getString("status")).isEqualTo(status);
    }

    @Given("a delivery in transit for order {string}")
    public void delivery_in_transit(String orderId) throws Exception {
        schedule_delivery(orderId, 44.0, 12.0, 44.1, 12.1, 2.0);
        currentDelivery = new JsonObject(post(deliveryUrl("start"), null).body());
    }

    @When("the delivery is completed")
    public void the_delivery_is_completed() throws Exception {
        currentDelivery = new JsonObject(post(deliveryUrl("complete"), null).body());
    }

    @Then("the delivery status is {string}")
    public void delivery_status_is(String status) {
        assertThat(currentDelivery.getString("status")).isEqualTo(status);
    }

    private String deliveryUrl(String action) {
        return DELIVERIES_ENDPOINT + "/" + currentDelivery.getString("id") + "/" + action;
    }

    private HttpResponse<String> post(String url, JsonObject body) throws Exception {
        var publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body.encode());
        var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(publisher)
            .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
