package sap.shipping.drone.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.drone.domain.*;
import java.util.Comparator;
import java.util.Optional;

@InBoundPort
public class DroneService {

    private final DroneRepository repository;
    private final DeliveryServicePort deliveryService;

    public DroneService(DroneRepository repository, DeliveryServicePort deliveryService) {
        this.repository = repository;
        this.deliveryService = deliveryService;
    }

    public Drone registerDrone(double maxWeightKg, double lat, double lng) {
        var drone = new Drone(DroneId.generate(), maxWeightKg, new Location(lat, lng));
        repository.save(drone);
        return drone;
    }

    public Optional<Drone> findAvailableDrone(double lat, double lng, double weightKg) {
        var pickupLocation = new Location(lat, lng);
        return repository.findAllAvailable().stream()
            .filter(d -> d.canCarry(weightKg))
            .min(Comparator.comparingDouble(d -> d.location().distanceTo(pickupLocation)));
    }

    public Drone assignDrone(DroneId droneId) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.assignToDelivery();
        repository.save(drone);
        drone.clearEvents();
        return drone;
    }

    public void updateLocation(DroneId droneId, double lat, double lng) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.updateLocation(new Location(lat, lng));
        repository.save(drone);
        deliveryService.notifyLocationUpdated(droneId.value(), lat, lng);
        drone.clearEvents();
    }

    public Drone releaseDrone(DroneId droneId) {
        var drone = repository.findById(droneId)
            .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
        drone.release();
        repository.save(drone);
        drone.clearEvents();
        return drone;
    }

    public Optional<Drone> getDrone(DroneId droneId) {
        return repository.findById(droneId);
    }
}
