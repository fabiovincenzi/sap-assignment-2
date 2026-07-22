package sap.shipping.gateway.domain;

/**
 * The gateway's view of a drone, projected from the Drone Service model.
 */
public record DroneView(
    String id,
    double maxWeightKg,
    double lat,
    double lng,
    String status
) {}
