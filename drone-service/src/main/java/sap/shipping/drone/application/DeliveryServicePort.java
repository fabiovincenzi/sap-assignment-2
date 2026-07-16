package sap.shipping.drone.application;

import sap.shipping.common.exagonal.OutBoundPort;

@OutBoundPort
public interface DeliveryServicePort {
    void notifyLocationUpdated(String droneId, double lat, double lng);
}
