package sap.shipping.drone.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.drone.domain.DroneId;
import sap.shipping.drone.domain.Location;

public record DroneLocationUpdated(
    DroneId droneId,
    Location location
) implements DomainEvent {}
