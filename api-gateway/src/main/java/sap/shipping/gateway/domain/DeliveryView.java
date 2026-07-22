package sap.shipping.gateway.domain;

/**
 * The gateway's view of a delivery, projected from the Delivery Service model.
 */
public record DeliveryView(
    String id,
    String orderId,
    String status,
    String droneId,
    double currentLat,
    double currentLng,
    int estimatedMinutes,
    String createdAt
) {}
