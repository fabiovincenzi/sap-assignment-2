package sap.shipping.gateway.infrastructure;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.gateway.application.OrderServicePort;
import sap.shipping.gateway.application.ServiceNotAvailableException;
import sap.shipping.gateway.domain.NewOrder;
import sap.shipping.gateway.domain.OrderView;
import sap.shipping.gateway.domain.UserView;

/**
 * Proxy for the Order Service, using synchronous HTTP.
 */
@Adapter
public class OrderServiceProxy extends HttpSyncBaseProxy implements OrderServicePort {

    static Logger logger = Logger.getLogger("[Gateway OrderProxy]");

    private final String serviceURI;

    public OrderServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public UserView register(String username, String password) {
        try {
            var body = new JsonObject().put("username", username).put("password", password);
            var response = doPost(serviceURI + "/api/users/register", body);
            logger.log(Level.INFO, "POST /api/users/register -> " + response.statusCode());
            if (response.statusCode() == 201) {
                var json = new JsonObject(response.body());
                return new UserView(json.getString("userId"), json.getString("username"));
            }
            throw new ServiceNotAvailableException("register failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "order-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("order-service not reachable");
        }
    }

    @Override
    public UserView login(String username, String password) {
        try {
            var body = new JsonObject().put("username", username).put("password", password);
            var response = doPost(serviceURI + "/api/users/login", body);
            logger.log(Level.INFO, "POST /api/users/login -> " + response.statusCode());
            if (response.statusCode() == 200) {
                var json = new JsonObject(response.body());
                return new UserView(json.getString("userId"), json.getString("username"));
            }
            throw new ServiceNotAvailableException("login failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "order-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("order-service not reachable");
        }
    }

    @Override
    public OrderView createOrder(NewOrder o) {
        try {
            var body = new JsonObject()
                .put("customerId", o.customerId())
                .put("pickupStreet", o.pickupStreet())
                .put("pickupLat", o.pickupLat())
                .put("pickupLng", o.pickupLng())
                .put("deliveryStreet", o.deliveryStreet())
                .put("deliveryLat", o.deliveryLat())
                .put("deliveryLng", o.deliveryLng())
                .put("weightKg", o.weightKg());
            var response = doPost(serviceURI + "/api/orders", body);
            logger.log(Level.INFO, "POST /api/orders -> " + response.statusCode());
            if (response.statusCode() == 201) {
                return toOrderView(new JsonObject(response.body()));
            }
            throw new ServiceNotAvailableException("createOrder failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "order-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("order-service not reachable");
        }
    }

    @Override
    public Optional<OrderView> getOrder(String orderId) {
        try {
            var response = doGet(serviceURI + "/api/orders/" + orderId);
            logger.log(Level.INFO, "GET /api/orders/" + orderId + " -> " + response.statusCode());
            if (response.statusCode() == 200) {
                return Optional.of(toOrderView(new JsonObject(response.body())));
            }
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            throw new ServiceNotAvailableException("getOrder failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "order-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("order-service not reachable");
        }
    }

    @Override
    public OrderView confirmOrder(String orderId) {
        return orderTransition(orderId, "confirm");
    }

    @Override
    public OrderView cancelOrder(String orderId) {
        return orderTransition(orderId, "cancel");
    }

    /** Shared body for confirm/cancel: same call shape, different path segment. */
    private OrderView orderTransition(String orderId, String action) {
        try {
            var response = doPost(serviceURI + "/api/orders/" + orderId + "/" + action, new JsonObject());
            logger.log(Level.INFO, "POST /api/orders/" + orderId + "/" + action + " -> " + response.statusCode());
            if (response.statusCode() == 200) {
                return toOrderView(new JsonObject(response.body()));
            }
            throw new ServiceNotAvailableException(action + " failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "order-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("order-service not reachable");
        }
    }

    private OrderView toOrderView(JsonObject j) {
        return new OrderView(
            j.getString("id"),
            j.getString("customerId"),
            j.getString("status"),
            j.getString("pickupStreet"),
            j.getString("deliveryStreet"),
            j.getDouble("weightKg", 0.0),
            j.getString("createdAt"));
    }
}
