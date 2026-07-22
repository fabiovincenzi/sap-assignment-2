package sap.shipping.gateway.domain;

/**
 * Result of the tracking aggregation: it combines data owned by the Order
 * Service (order status) and by the Delivery Service (drone position and ETA)
 * into a single view for the client. 
 *
 * When the order has no delivery yet (e.g. still PENDING), the delivery-related
 * fields are absent and hasDelivery is false.
 */
public record TrackingInfo(
    String orderId,
    String orderStatus,
    boolean hasDelivery,
    String deliveryStatus,
    String droneId,
    double currentLat,
    double currentLng,
    int estimatedMinutes
) {
    /** Tracking for an order whose delivery has not been scheduled yet. */
    public static TrackingInfo orderOnly(String orderId, String orderStatus) {
        return new TrackingInfo(orderId, orderStatus, false, null, null, 0, 0, 0);
    }
}
