package sap.shipping.order.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.DeliveryServicePort;
import sap.shipping.order.domain.events.OrderConfirmed;

@Adapter
public class DeliveryServiceProxy implements DeliveryServicePort {

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

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
