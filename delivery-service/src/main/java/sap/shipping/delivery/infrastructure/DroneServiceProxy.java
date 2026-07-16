package sap.shipping.delivery.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DroneServicePort;
import java.util.Optional;

@Adapter
public class DroneServiceProxy implements DroneServicePort {

    private final String serviceURI;

    public DroneServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public Optional<String> requestAvailableDrone(double lat, double lng, double weightKg) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceURI + "/api/drones/available?lat=" + lat + "&lng=" + lng + "&weightKg=" + weightKg))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var body = new JsonObject(response.body());
                return Optional.of(body.getString("droneId"));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void releaseDrone(String droneId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceURI + "/api/drones/" + droneId + "/release"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
