package sap.shipping.delivery.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.Route;
import java.time.Instant;

/**
 * Creation event: carries the full initial state, so that replaying it rebuilds the aggregate.
 */
public record DeliveryScheduled(
    DeliveryId deliveryId,
    String orderId,
    Route route,
    double weightKg,
    Instant createdAt
) implements DomainEvent {}
