package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.delivery.domain.*;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@InBoundPort
public class DeliveryService {

    static Logger logger = Logger.getLogger("[Delivery Service]");

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
        logger.log(Level.INFO, "schedule delivery " + delivery.getId().value() + " for order " + orderId);

        var droneId = droneService.requestAvailableDrone(pickupLat, pickupLng, weightKg);
        if (droneId.isPresent()) {
            delivery.assignDrone(droneId.get());
            repository.save(delivery);
            logger.log(Level.INFO, "drone " + droneId.get() + " assigned to delivery " + delivery.getId().value());
        } else {
            logger.log(Level.WARNING, "no drone available for delivery " + delivery.getId().value());
        }

        return delivery;
    }

    public Delivery startDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.startTransit();
        repository.save(delivery);
        logger.log(Level.INFO, "start delivery " + deliveryId.value());
        return delivery;
    }

    public void updateDronePosition(String droneId, double lat, double lng) {
        repository.findByDroneId(droneId).ifPresent(delivery -> {
            delivery.updatePosition(lat, lng);
            repository.save(delivery);
            logger.log(Level.INFO, "update position of delivery " + delivery.getId().value()
                + " to (" + lat + ", " + lng + ")");
        });
    }

    public Delivery completeDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.complete();
        repository.save(delivery);
        logger.log(Level.INFO, "complete delivery " + deliveryId.value());

        delivery.pendingEvents().stream()
            .filter(e -> e instanceof DeliveryCompleted)
            .map(e -> (DeliveryCompleted) e)
            .forEach(e -> {
                logger.log(Level.INFO, "notifying delivery-completed for order " + e.orderId()
                    + " and releasing drone " + e.droneId());
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
