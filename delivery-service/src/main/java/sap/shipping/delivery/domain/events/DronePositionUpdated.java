package sap.shipping.delivery.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;

public record DronePositionUpdated(
    DeliveryId deliveryId,
    double lat,
    double lng
) implements DomainEvent {}
