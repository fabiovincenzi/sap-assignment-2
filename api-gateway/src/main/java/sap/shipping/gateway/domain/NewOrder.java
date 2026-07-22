package sap.shipping.gateway.domain;

/**
 * Input data the gateway needs to place a new order on behalf of a client.
 */
public record NewOrder(
    String customerId,
    String pickupStreet,
    double pickupLat,
    double pickupLng,
    String deliveryStreet,
    double deliveryLat,
    double deliveryLng,
    double weightKg
) {}
