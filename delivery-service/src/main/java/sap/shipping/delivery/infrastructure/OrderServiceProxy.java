package sap.shipping.delivery.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.OrderServicePort;

@Adapter
public class OrderServiceProxy implements OrderServicePort {

    static Logger logger = Logger.getLogger("[OrderServiceProxy]");

    private final String serviceURI;

    public OrderServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public void notifyDeliveryCompleted(String orderId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceURI + "/api/orders/" + orderId + "/complete"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

            logger.log(Level.INFO, "POST " + serviceURI + "/api/orders/" + orderId + "/complete");
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.log(Level.INFO, "Response code: " + response.statusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
