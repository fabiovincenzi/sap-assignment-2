package sap.shipping.delivery.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;

public record DroneAssigned(
    DeliveryId deliveryId,
    String droneId
) implements DomainEvent {}
