package sap.shipping.drone.application;

import sap.shipping.common.exagonal.OutBoundPort;

@OutBoundPort
public interface DroneServiceObserver {
    void notifyDroneAvailable(String droneId);
    void notifyDroneAssigned(String droneId);
}
