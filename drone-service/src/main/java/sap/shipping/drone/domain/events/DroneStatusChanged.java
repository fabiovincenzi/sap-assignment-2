package sap.shipping.drone.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.drone.domain.DroneId;
import sap.shipping.drone.domain.DroneStatus;

public record DroneStatusChanged(
    DroneId droneId,
    DroneStatus newStatus
) implements DomainEvent {}
