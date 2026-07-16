package sap.shipping.delivery.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;

public record DeliveryCompleted(
    DeliveryId deliveryId,
    String orderId,
    String droneId
) implements DomainEvent {}
