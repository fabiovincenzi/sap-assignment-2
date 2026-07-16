package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.delivery.domain.*;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import java.util.Optional;

@InBoundPort
public class DeliveryService {

    private final DeliveryRepository repository;
    private final DroneServicePort droneService;
    private final OrderServicePort orderService;

    public DeliveryService(DeliveryRepository repository, DroneServicePort droneService, OrderServicePort orderService) {
        this.repository = repository;
        this.droneService = droneService;
        this.orderService = orderService;
    }

    public Delivery scheduleDelivery(String orderId, double pickupLat, double pickupLng,
                                      double deliveryLat, double deliveryLng, double weightKg) {
        var route = new Route(pickupLat, pickupLng, deliveryLat, deliveryLng);
        var delivery = new Delivery(DeliveryId.generate(), orderId, route, weightKg);
        repository.save(delivery);

        var droneId = droneService.requestAvailableDrone(pickupLat, pickupLng, weightKg);
        droneId.ifPresent(id -> {
            delivery.assignDrone(id);
            repository.save(delivery);
        });

        return delivery;
    }

    public Delivery startDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.startTransit();
        repository.save(delivery);
        return delivery;
    }

    public void updateDronePosition(String droneId, double lat, double lng) {
        repository.findByDroneId(droneId).ifPresent(delivery -> {
            delivery.updatePosition(lat, lng);
            repository.save(delivery);
        });
    }

    public Delivery completeDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.complete();
        repository.save(delivery);

        delivery.pendingEvents().stream()
            .filter(e -> e instanceof DeliveryCompleted)
            .map(e -> (DeliveryCompleted) e)
            .forEach(e -> {
                orderService.notifyDeliveryCompleted(e.orderId());
                droneService.releaseDrone(e.droneId());
            });
        delivery.clearEvents();

        return delivery;
    }

    public Optional<Delivery> trackDelivery(DeliveryId deliveryId) {
        return repository.findById(deliveryId);
    }

    public Optional<Delivery> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId);
    }
}
