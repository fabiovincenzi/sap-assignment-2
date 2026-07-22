package sap.shipping.gateway.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.json.JsonObject;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.gateway.application.DroneServicePort;
import sap.shipping.gateway.application.ServiceNotAvailableException;
import sap.shipping.gateway.domain.DroneView;

/**
 * Proxy for the Drone Service, using synchronous HTTP.
 */
@Adapter
public class DroneServiceProxy extends HttpSyncBaseProxy implements DroneServicePort {

    static Logger logger = Logger.getLogger("[Gateway DroneProxy]");

    private final String serviceURI;

    public DroneServiceProxy(String host, int port) {
        this.serviceURI = "http://" + host + ":" + port;
    }

    @Override
    public DroneView registerDrone(double maxWeightKg, double lat, double lng) {
        try {
            var body = new JsonObject()
                .put("maxWeightKg", maxWeightKg)
                .put("lat", lat)
                .put("lng", lng);
            var response = doPost(serviceURI + "/api/drones", body);
            logger.log(Level.INFO, "POST /api/drones -> " + response.statusCode());
            if (response.statusCode() == 201) {
                var j = new JsonObject(response.body());
                return new DroneView(
                    j.getString("id"),
                    j.getDouble("maxWeightKg", 0.0),
                    j.getDouble("lat", 0.0),
                    j.getDouble("lng", 0.0),
                    j.getString("status"));
            }
            throw new ServiceNotAvailableException("registerDrone failed: HTTP " + response.statusCode());
        } catch (ServiceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "drone-service not reachable at " + serviceURI + " - " + e.getMessage());
            throw new ServiceNotAvailableException("drone-service not reachable");
        }
    }
}
