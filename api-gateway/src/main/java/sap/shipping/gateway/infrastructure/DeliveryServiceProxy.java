package sap.shipping.gateway.infrastructure;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.gateway.application.DeliveryServicePort;
import sap.shipping.gateway.application.ServiceNotAvailableException;
import sap.shipping.gateway.domain.DeliveryView;

/**
 * Proxy for the Delivery Service, using synchronous HTTP.
 */
@Adapter
public class DeliveryServiceProxy extends HttpSyncBaseProxy implements DeliveryServicePort {

    static Logger logger = Logger.getLogger("[Gateway DeliveryProxy]");

    private final String serviceURI;

    public DeliveryServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public Optional<DeliveryView> findByOrderId(String orderId) {
        try {
            var response = doGet(serviceURI + "/api/deliveries/order/" + orderId);
            logger.log(Level.INFO, "GET /api/deliveries/order/" + orderId + " -> " + response.statusCode());
            if (response.statusCode() == 200) {
                var j = new JsonObject(response.body());
                return Optional.of(new DeliveryView(
                    j.getString("id"),
                    j.getString("orderId"),
                    j.getString("status"),
                    j.getString("droneId"),
                    j.getDouble("currentLat", 0.0),
                    j.getDouble("currentLng", 0.0),
                    j.getInteger("estimatedMinutes", 0),
                    j.getString("createdAt")));
            }
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            throw new ServiceNotAvailableException("findByOrderId failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "delivery-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("delivery-service not reachable");
        }
    }
}
