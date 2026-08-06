package system_end_to_end_tests;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Turns two quality attribute scenarios into executable checks, reading the observability
 * patterns the system already exposes: the health of each target (Prometheus 'up', fed by the
 * Health Check API) and the application metrics of the services.
 *
 * The system must be up: docker compose up -d
 */
@DisplayName("Quality attribute scenarios")
public class QualityAttributeScenariosTest {

    private static final String GATEWAY = "http://localhost:8080";
    private static final String API = GATEWAY + "/api/v1";
    private static final String PROMETHEUS = "http://localhost:9090";
    private static final String DRONE_METRICS = "http://localhost:9493/metrics";

    /** Response measure declared by QAS-A. */
    private static final Duration DETECTION_BUDGET = Duration.ofSeconds(15);
    private static final File REPO_ROOT = new File("..");

    /**
     * QAS-A - Availability: a service going down must be detected, and must not stop the
     * system from taking orders.
     */
    @Test
    @DisplayName("A: a failing service is detected and its failure does not propagate")
    public void aFailingServiceIsDetectedAndDoesNotPropagate() throws Exception {
        assertThat(droneServiceIsUp()).as("the drone service must be up to begin with").isTrue();

        docker("stop", "drone-service");
        try {
            var detectedIn = awaitDroneService(false);
            assertThat(detectedIn)
                .as("the failure must be visible in the metrics within the budget")
                .isLessThan(DETECTION_BUDGET);
            System.out.println("### QAS-A - failure detected in " + detectedIn.toMillis() + " ms");

            var tracking = placeAndConfirmAnOrder();

            assertThat(tracking.getBoolean("hasDelivery"))
                .as("the order must still be accepted and a delivery scheduled")
                .isTrue();
            assertThat(tracking.getString("deliveryStatus")).isEqualTo("SCHEDULED");
            assertThat(tracking.getString("droneId")).isNull();
            System.out.println("### QAS-A - degraded but operational: " + tracking.encode());
        } finally {
            docker("start", "drone-service");
            awaitDroneService(true);
        }
    }

    /**
     * QAS-B - Resource saturation: with every drone busy the system keeps accepting orders,
     * leaving the deliveries waiting instead of rejecting them.
     */
    @Test
    @DisplayName("B: with no drone available the orders are still scheduled")
    public void withNoDroneAvailableTheOrdersAreStillScheduled() throws Exception {
        registerADrone();
        assertThat(availableDrones()).as("at least one drone must be available").isGreaterThan(0);

        /* every confirmed order takes one drone: keep going until they run out */
        while (availableDrones() > 0) {
            placeAndConfirmAnOrder();
        }
        assertThat(availableDrones()).isZero();
        System.out.println("### QAS-B - fleet saturated: drones_available = 0");

        var tracking = placeAndConfirmAnOrder();

        assertThat(tracking.getBoolean("hasDelivery"))
            .as("saturation must not reject the order")
            .isTrue();
        assertThat(tracking.getString("deliveryStatus")).isEqualTo("SCHEDULED");
        assertThat(tracking.getString("droneId")).isNull();
        System.out.println("### QAS-B - order accepted with no drone: " + tracking.encode());
    }

    // --- the system, driven through the gateway ---

    private JsonObject placeAndConfirmAnOrder() throws Exception {
        var username = "qas-" + UUID.randomUUID().toString().substring(0, 8);
        var registered = post(API + "/users/register",
            new JsonObject().put("username", username).put("password", "Secret#123"));
        var customerId = new JsonObject(registered.body()).getString("userId");

        var created = post(API + "/orders", new JsonObject()
            .put("customerId", customerId)
            .put("pickupStreet", "Via Roma 1").put("pickupLat", 44.14).put("pickupLng", 12.24)
            .put("deliveryStreet", "Via Verdi 9").put("deliveryLat", 44.15).put("deliveryLng", 12.25)
            .put("weightKg", 2.5));
        assertThat(created.statusCode()).isEqualTo(201);
        var orderId = new JsonObject(created.body()).getString("id");

        assertThat(post(API + "/orders/" + orderId + "/confirm", null).statusCode()).isEqualTo(200);

        return new JsonObject(get(API + "/tracking/" + orderId).body());
    }

    private void registerADrone() throws Exception {
        var response = post(API + "/drones",
            new JsonObject().put("maxWeightKg", 5.0).put("lat", 44.14).put("lng", 12.24));
        assertThat(response.statusCode()).isEqualTo(201);
    }

    // --- observability: health of the targets and application metrics ---

    /** Prometheus 'up', which is 1 while the target answers the scrape and 0 as soon as it stops. */
    private boolean droneServiceIsUp() throws Exception {
        var query = URLEncoder.encode("up{job=\"monitoring-drone-service\"}", StandardCharsets.UTF_8);
        var response = get(PROMETHEUS + "/api/v1/query?query=" + query);
        var result = new JsonObject(response.body()).getJsonObject("data").getJsonArray("result");
        if (result.isEmpty()) {
            return false;
        }
        return "1".equals(result.getJsonObject(0).getJsonArray("value").getString(1));
    }

    private Duration awaitDroneService(boolean up) throws Exception {
        var start = Instant.now();
        while (Duration.between(start, Instant.now()).compareTo(DETECTION_BUDGET) < 0) {
            if (droneServiceIsUp() == up) {
                return Duration.between(start, Instant.now());
            }
            Thread.sleep(500);
        }
        throw new AssertionError("the drone service never became " + (up ? "up" : "down")
            + " within " + DETECTION_BUDGET.toSeconds() + "s");
    }

    /** Application metric of the drone service, read at the source to avoid the scrape delay. */
    private int availableDrones() throws Exception {
        return get(DRONE_METRICS).body().lines()
            .filter(line -> line.startsWith("drones_available "))
            .map(line -> (int) Double.parseDouble(line.split(" ")[1]))
            .findFirst()
            .orElseThrow(() -> new AssertionError("metric drones_available not exposed"));
    }

    // --- plumbing ---

    private void docker(String... args) throws Exception {
        var command = new java.util.ArrayList<String>(java.util.List.of("docker", "compose"));
        command.addAll(java.util.List.of(args));
        var exitCode = new ProcessBuilder(command).directory(REPO_ROOT).inheritIO().start().waitFor();
        assertThat(exitCode).as("docker compose " + String.join(" ", args)).isZero();
    }

    private HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, JsonObject body) throws Exception {
        var publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body.encode());
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json").POST(publisher).build(),
            HttpResponse.BodyHandlers.ofString());
    }
}
