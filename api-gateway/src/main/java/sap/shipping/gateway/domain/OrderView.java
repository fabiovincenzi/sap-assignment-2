package sap.shipping.gateway.domain;

/**
 * The gateway's view of an order. It is a flat, read-only projection of the
 * Order Service model: only the fields the gateway needs to serve its clients.
 */
public record OrderView(
    String id,
    String customerId,
    String status,
    String pickupStreet,
    String deliveryStreet,
    double weightKg,
    String createdAt
) {}
