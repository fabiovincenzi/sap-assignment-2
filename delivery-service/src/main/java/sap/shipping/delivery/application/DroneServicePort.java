package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.OutBoundPort;
import java.util.Optional;

@OutBoundPort
public interface DroneServicePort {
    Optional<String> requestAvailableDrone(double lat, double lng, double weightKg);
    void releaseDrone(String droneId);
}
