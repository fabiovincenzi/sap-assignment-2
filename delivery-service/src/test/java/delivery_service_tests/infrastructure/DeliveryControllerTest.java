package delivery_service_tests.infrastructure;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.*;
import sap.shipping.delivery.infrastructure.DeliveryController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;

/**
 * Solitary unit test of the controller: the service behind it is a mock, so what is under
 * test is only the adapter - routes, payload parsing, status codes and JSON produced.
 */
@DisplayName("Delivery Controller test case")
public class DeliveryControllerTest {

    private static final int TEST_PORT = 9591;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT + "/api/deliveries";

    private Vertx vertx;
    private DeliveryServiceMock serviceMock;

    @BeforeEach
    public void setUp() throws Exception {
        var sync = new Synchroniser();
        vertx = Vertx.vertx();
        serviceMock = new DeliveryServiceMock();
        var controller = new DeliveryController(serviceMock);

        vertx.createHttpServer()
            .requestHandler(controller.createRouter(vertx))
            .listen(TEST_PORT)
            .onSuccess(server -> sync.notifySync());

        sync.awaitSync();
    }

    @AfterEach
    public void tearDown() {
        vertx.close();
    }

    @Test
    @DisplayName("Scheduling a delivery replies 201 with the delivery")
    public void schedulingADeliveryRepliesCreated() throws Exception {
        var body = new JsonObject()
            .put("orderId", "order-1")
            .put("pickupLat", 44.0).put("pickupLng", 12.0)
            .put("deliveryLat", 44.1).put("deliveryLng", 12.1)
            .put("weightKg", 2.5);

        var response = post(BASE_URL, body);

        assertThat(response.statusCode()).isEqualTo(201);
        var json = new JsonObject(response.body());
        assertThat(json.getString("id")).isEqualTo(DeliveryServiceMock.KNOWN_DELIVERY);
        assertThat(json.getString("status")).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("A malformed payload replies 400 instead of failing")
    public void aMalformedPayloadRepliesBadRequest() throws Exception {
        var response = post(BASE_URL, new JsonObject().put("orderId", "order-1"));

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Tracking an unknown delivery replies 404")
    public void trackingAnUnknownDeliveryRepliesNotFound() throws Exception {
        var response = get(BASE_URL + "/does-not-exist");

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Tracking a known delivery replies its JSON")
    public void trackingAKnownDeliveryRepliesTheDelivery() throws Exception {
        var response = get(BASE_URL + "/" + DeliveryServiceMock.KNOWN_DELIVERY);

        assertThat(response.statusCode()).isEqualTo(200);
        var json = new JsonObject(response.body());
        assertThat(json.getString("orderId")).isEqualTo(DeliveryServiceMock.KNOWN_ORDER);
        assertThat(json.fieldNames())
            .contains("id", "orderId", "status", "currentLat", "currentLng", "estimatedMinutes", "createdAt");
    }

    @Test
    @DisplayName("The drone position is forwarded to the service as sent")
    public void theDronePositionIsForwardedToTheService() throws Exception {
        var body = new JsonObject().put("droneId", "drone-1").put("lat", 44.05).put("lng", 12.05);

        var response = post(BASE_URL + "/drone-position", body);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(serviceMock.lastDroneId).isEqualTo("drone-1");
        assertThat(serviceMock.lastLat).isEqualTo(44.05);
        assertThat(serviceMock.lastLng).isEqualTo(12.05);
    }

    @Test
    @DisplayName("The health endpoint reports the service as UP")
    public void theHealthEndpointReportsUp() throws Exception {
        var response = get("http://localhost:" + TEST_PORT + "/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new JsonObject(response.body()).getString("status")).isEqualTo("UP");
    }

    private HttpResponse<String> post(String url, JsonObject body) throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.encode()))
            .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String url) throws Exception {
        var request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
