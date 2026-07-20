package sap.shipping.drone.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.drone.application.DeliveryServicePort;

@Adapter
public class DeliveryServiceProxy implements DeliveryServicePort {

    static Logger logger = Logger.getLogger("[DeliveryServiceProxy]");

    private final String serviceURI;

    public DeliveryServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public void notifyLocationUpdated(String droneId, double lat, double lng) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            var body = new JsonObject()
                .put("droneId", droneId)
                .put("lat", lat)
                .put("lng", lng);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceURI + "/api/deliveries/drone-position"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

            logger.log(Level.INFO, "POST " + serviceURI + "/api/deliveries/drone-position - drone: " + droneId);
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log(Level.INFO, "Response code: " + response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
