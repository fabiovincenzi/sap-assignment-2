package sap.shipping.delivery.domain;

import sap.shipping.common.ddd.Aggregate;
import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import sap.shipping.delivery.domain.events.DeliveryFailed;
import sap.shipping.delivery.domain.events.DeliveryScheduled;
import sap.shipping.delivery.domain.events.DronePositionUpdated;
import sap.shipping.delivery.domain.events.DroneAssigned;
import sap.shipping.delivery.domain.events.TransitStarted;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Delivery aggregate, structured for event sourcing: process* validates and returns events
 * without changing the state, apply updates the state and never fails.
 * State is rebuilt by replaying events, so apply must stay deterministic.
 */
public class Delivery implements Aggregate<DeliveryId> {

    private DeliveryId id;
    private String orderId;
    private Route route;
    private double weightKg;
    private Instant createdAt;
    private DeliveryStatus status;
    private String droneId;
    private double currentLat;
    private double currentLng;
    private long version;
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    /** Empty aggregate, used to rebuild the state by replaying events. */
    public Delivery() {
    }

    /** Rebuilds an aggregate from a snapshot: only the events after it still have to be applied. */
    public static Delivery fromSnapshot(DeliverySnapshot s) {
        var delivery = new Delivery();
        delivery.id = s.deliveryId();
        delivery.orderId = s.orderId();
        delivery.route = s.route();
        delivery.weightKg = s.weightKg();
        delivery.createdAt = s.createdAt();
        delivery.status = s.status();
        delivery.droneId = s.droneId();
        delivery.currentLat = s.currentLat();
        delivery.currentLng = s.currentLng();
        delivery.version = s.version();
        return delivery;
    }

    public Delivery(DeliveryId id, String orderId, Route route, double weightKg) {
        this();
        emit(processSchedule(id, orderId, route, weightKg, Instant.now()));
    }

    // --- process: validate, produce events, do not mutate ---

    public List<DomainEvent> processSchedule(DeliveryId id, String orderId, Route route,
                                             double weightKg, Instant createdAt) {
        return List.of(new DeliveryScheduled(id, orderId, route, weightKg, createdAt));
    }

    public List<DomainEvent> processAssignDrone(String droneId) {
        if (status != DeliveryStatus.SCHEDULED) {
            throw new IllegalStateException("Can only assign drone to a SCHEDULED delivery");
        }
        return List.of(new DroneAssigned(id, droneId));
    }

    public List<DomainEvent> processStartTransit() {
        if (status != DeliveryStatus.DRONE_ASSIGNED) {
            throw new IllegalStateException("Can only start transit from DRONE_ASSIGNED status");
        }
        return List.of(new TransitStarted(id));
    }

    public List<DomainEvent> processUpdatePosition(double lat, double lng) {
        return List.of(new DronePositionUpdated(id, lat, lng));
    }

    public List<DomainEvent> processComplete() {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new IllegalStateException("Can only complete an IN_TRANSIT delivery");
        }
        return List.of(new DeliveryCompleted(id, orderId, droneId));
    }

    public List<DomainEvent> processFail(String reason) {
        return List.of(new DeliveryFailed(id, orderId, reason));
    }

    // --- apply: mutate, never fail ---

    public void apply(DomainEvent event) {
        if (event instanceof DeliveryScheduled e) {
            apply(e);
        } else if (event instanceof DroneAssigned e) {
            apply(e);
        } else if (event instanceof TransitStarted e) {
            apply(e);
        } else if (event instanceof DronePositionUpdated e) {
            apply(e);
        } else if (event instanceof DeliveryCompleted e) {
            apply(e);
        } else if (event instanceof DeliveryFailed e) {
            apply(e);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getName());
        }
        version++;
    }

    public void apply(DeliveryScheduled e) {
        this.id = e.deliveryId();
        this.orderId = e.orderId();
        this.route = e.route();
        this.weightKg = e.weightKg();
        this.createdAt = e.createdAt();
        this.status = DeliveryStatus.SCHEDULED;
        this.currentLat = e.route().pickupLat();
        this.currentLng = e.route().pickupLng();
    }

    public void apply(DroneAssigned e) {
        this.droneId = e.droneId();
        this.status = DeliveryStatus.DRONE_ASSIGNED;
    }

    public void apply(TransitStarted e) {
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    public void apply(DronePositionUpdated e) {
        this.currentLat = e.lat();
        this.currentLng = e.lng();
    }

    public void apply(DeliveryCompleted e) {
        this.status = DeliveryStatus.DELIVERED;
    }

    public void apply(DeliveryFailed e) {
        this.status = DeliveryStatus.FAILED;
    }

    // --- commands: process, apply, keep the events for persistence ---

    public void assignDrone(String droneId) {
        emit(processAssignDrone(droneId));
    }

    public void startTransit() {
        emit(processStartTransit());
    }

    public void updatePosition(double lat, double lng) {
        emit(processUpdatePosition(lat, lng));
    }

    public void complete() {
        emit(processComplete());
    }

    public void fail(String reason) {
        emit(processFail(reason));
    }

    private void emit(List<DomainEvent> events) {
        events.forEach(this::apply);
        pendingEvents.addAll(events);
    }

    // --- state ---

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

    /** Takes a picture of the current state, to be replayed from instead of the whole stream. */
    public DeliverySnapshot snapshot() {
        return new DeliverySnapshot(id, orderId, route, weightKg, createdAt,
            status, droneId, currentLat, currentLng, version);
    }

    /** Number of events applied so far, pending ones included. */
    public long version() { return version; }

    /** Version the aggregate was loaded at, i.e. the one the event store is expected to be at. */
    public long persistedVersion() { return version - pendingEvents.size(); }

    public List<DomainEvent> pendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearEvents() {
        pendingEvents.clear();
    }
}
