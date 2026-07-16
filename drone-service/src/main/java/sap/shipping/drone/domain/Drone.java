package sap.shipping.drone.domain;

import sap.shipping.common.ddd.Aggregate;
import sap.shipping.drone.domain.events.DroneLocationUpdated;
import sap.shipping.drone.domain.events.DroneStatusChanged;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Drone implements Aggregate<DroneId> {

    private final DroneId id;
    private final double maxWeightKg;
    private Location location;
    private DroneStatus status;
    private final List<Object> pendingEvents = new ArrayList<>();

    public Drone(DroneId id, double maxWeightKg, Location location) {
        this.id = id;
        this.maxWeightKg = maxWeightKg;
        this.location = location;
        this.status = DroneStatus.AVAILABLE;
    }

    @Override
    public DroneId getId() { return id; }

    public double maxWeightKg() { return maxWeightKg; }
    public Location location() { return location; }
    public DroneStatus status() { return status; }

    public boolean canCarry(double weightKg) {
        return weightKg <= maxWeightKg;
    }

    public boolean isAvailable() {
        return status == DroneStatus.AVAILABLE;
    }

    public void assignToDelivery() {
        if (status != DroneStatus.AVAILABLE) {
            throw new IllegalStateException("Drone is not available");
        }
        this.status = DroneStatus.IN_FLIGHT;
        pendingEvents.add(new DroneStatusChanged(id, DroneStatus.IN_FLIGHT));
    }

    public void updateLocation(Location newLocation) {
        this.location = newLocation;
        pendingEvents.add(new DroneLocationUpdated(id, newLocation));
    }

    public void release() {
        this.status = DroneStatus.AVAILABLE;
        pendingEvents.add(new DroneStatusChanged(id, DroneStatus.AVAILABLE));
    }

    public List<Object> pendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearEvents() {
        pendingEvents.clear();
    }
}
