package sap.shipping.delivery.domain;

import sap.shipping.common.ddd.Aggregate;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import sap.shipping.delivery.domain.events.DeliveryFailed;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Delivery implements Aggregate<DeliveryId> {

    private final DeliveryId id;
    private final String orderId;
    private final Route route;
    private final double weightKg;
    private final Instant createdAt;
    private DeliveryStatus status;
    private String droneId;
    private double currentLat;
    private double currentLng;
    private final List<Object> pendingEvents = new ArrayList<>();

    public Delivery(DeliveryId id, String orderId, Route route, double weightKg) {
        this.id = id;
        this.orderId = orderId;
        this.route = route;
        this.weightKg = weightKg;
        this.createdAt = Instant.now();
        this.status = DeliveryStatus.SCHEDULED;
        this.currentLat = route.pickupLat();
        this.currentLng = route.pickupLng();
    }

    @Override
    public DeliveryId getId() { return id; }

    public String orderId() { return orderId; }
    public Route route() { return route; }
    public double weightKg() { return weightKg; }
    public DeliveryStatus status() { return status; }
    public String droneId() { return droneId; }
    public Instant createdAt() { return createdAt; }
    public double currentLat() { return currentLat; }
    public double currentLng() { return currentLng; }

    public long estimatedMinutes() {
        return route.estimatedMinutes();
    }

    public void assignDrone(String droneId) {
        if (status != DeliveryStatus.SCHEDULED) {
            throw new IllegalStateException("Can only assign drone to a SCHEDULED delivery");
        }
        this.droneId = droneId;
        this.status = DeliveryStatus.DRONE_ASSIGNED;
    }

    public void startTransit() {
        if (status != DeliveryStatus.DRONE_ASSIGNED) {
            throw new IllegalStateException("Can only start transit from DRONE_ASSIGNED status");
        }
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    public void updatePosition(double lat, double lng) {
        this.currentLat = lat;
        this.currentLng = lng;
    }

    public void complete() {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("Can only complete an IN_TRANSIT delivery");
        }
        this.status = DeliveryStatus.DELIVERED;
        pendingEvents.add(new DeliveryCompleted(id, orderId, droneId));
    }

    public void fail(String reason) {
        this.status = DeliveryStatus.FAILED;
        pendingEvents.add(new DeliveryFailed(id, orderId, reason));
    }

    public List<Object> pendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearEvents() {
        pendingEvents.clear();
    }
}
