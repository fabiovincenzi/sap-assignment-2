package sap.shipping.drone.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.drone.domain.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@InBoundPort
public class DroneService {

    static Logger logger = Logger.getLogger("[Drone Service]");

    private final DroneRepository repository;
    private final DeliveryServicePort deliveryService;
    private final List<DroneServiceObserver> observers = new ArrayList<>();

    public DroneService(DroneRepository repository, DeliveryServicePort deliveryService) {
        this.repository = repository;
        this.deliveryService = deliveryService;
    }

    public void addObserver(DroneServiceObserver observer) {
        observers.add(observer);
    }

    public Drone registerDrone(double maxWeightKg, double lat, double lng) {
        var drone = new Drone(DroneId.generate(), maxWeightKg, new Location(lat, lng));
        repository.save(drone);
        logger.log(Level.INFO, "register new drone " + drone.getId().value() + " - max weight " + maxWeightKg + "kg");
        observers.forEach(o -> o.notifyDroneAvailable(drone.getId().value()));
        return drone;
    }

    public Optional<Drone> findAvailableDrone(double lat, double lng, double weightKg) {
        var pickupLocation = new Location(lat, lng);
        var found = repository.findAllAvailable().stream()
            .filter(d -> d.canCarry(weightKg))
            .min(Comparator.comparingDouble(d -> d.location().distanceTo(pickupLocation)));
        logger.log(Level.INFO, "find available drone for " + weightKg + "kg - "
            + found.map(d -> "found " + d.getId().value()).orElse("none available"));
        return found;
    }

    public Drone assignDrone(DroneId droneId) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.assignToDelivery();
        repository.save(drone);
        logger.log(Level.INFO, "assign drone " + droneId.value() + " to a delivery");
        observers.forEach(o -> o.notifyDroneAssigned(droneId.value()));
        drone.clearEvents();
        return drone;
    }

    public void updateLocation(DroneId droneId, double lat, double lng) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.updateLocation(new Location(lat, lng));
        repository.save(drone);
        logger.log(Level.INFO, "update location of drone " + droneId.value() + " to (" + lat + ", " + lng + ")");
        deliveryService.notifyLocationUpdated(droneId.value(), lat, lng);
        drone.clearEvents();
    }

    public Drone releaseDrone(DroneId droneId) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.release();
        repository.save(drone);
        logger.log(Level.INFO, "release drone " + droneId.value());
        observers.forEach(o -> o.notifyDroneAvailable(droneId.value()));
        drone.clearEvents();
        return drone;
    }

    public Optional<Drone> getDrone(DroneId droneId) {
        return repository.findById(droneId);
    }
}
