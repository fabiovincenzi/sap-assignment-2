package sap.shipping.delivery.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;

public record DeliveryFailed(
    DeliveryId deliveryId,
    String orderId,
    String reason
) implements DomainEvent {}
