package sap.shipping.order.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.DeliveryServicePort;
import sap.shipping.order.domain.events.OrderConfirmed;

@Adapter
public class DeliveryServiceProxy implements DeliveryServicePort {

    static Logger logger = Logger.getLogger("[DeliveryServiceProxy]");

    private final String serviceURI;

    public DeliveryServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public void notifyOrderConfirmed(OrderConfirmed event) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            var body = new JsonObject()
                .put("orderId", event.orderId().value())
                .put("pickupStreet", event.pickup().street())
                .put("pickupLat", event.pickup().lat())
                .put("pickupLng", event.pickup().lng())
                .put("deliveryStreet", event.delivery().street())
                .put("deliveryLat", event.delivery().lat())
                .put("deliveryLng", event.delivery().lng())
                .put("weightKg", event.packageInfo().weightKg());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceURI + "/api/deliveries"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

            logger.log(Level.INFO, "POST " + serviceURI + "/api/deliveries - order: " + event.orderId().value());
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log(Level.INFO, "Response code: " + response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
