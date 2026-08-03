package sap.shipping.delivery.domain;

import java.time.Instant;

/**
 * State of a delivery at a given version, kept to avoid replaying the whole stream.
 * It is derived data: dropping every snapshot leaves the service correct, only slower.
 */
public record DeliverySnapshot(
    DeliveryId deliveryId,
    String orderId,
    Route route,
    double weightKg,
    Instant createdAt,
    DeliveryStatus status,
    String droneId,
    double currentLat,
    double currentLng,
    long version
) {}
