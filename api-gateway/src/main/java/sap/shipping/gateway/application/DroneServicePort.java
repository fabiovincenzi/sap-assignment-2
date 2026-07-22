package sap.shipping.gateway.application;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.gateway.domain.DroneView;

/**
 * What the gateway needs from the Drone Service: fleet registration of drones.
 */
@OutBoundPort
public interface DroneServicePort {

    DroneView registerDrone(double maxWeightKg, double lat, double lng) throws ServiceNotAvailableException;
}
