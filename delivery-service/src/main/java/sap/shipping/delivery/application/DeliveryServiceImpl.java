package sap.shipping.delivery.application;

import sap.shipping.delivery.domain.*;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeliveryServiceImpl implements DeliveryService {

    static Logger logger = Logger.getLogger("[Delivery Service]");

    private final DeliveryRepository repository;
    private final DroneServicePort droneService;
    private final OrderServicePort orderService;
    private final List<DeliveryServiceObserver> observers = new ArrayList<>();

    public DeliveryServiceImpl(DeliveryRepository repository, DroneServicePort droneService, OrderServicePort orderService) {
        this.repository = repository;
        this.droneService = droneService;
        this.orderService = orderService;
    }

    @Override
    public void addObserver(DeliveryServiceObserver observer) {
        observers.add(observer);
    }

    @Override
    public Delivery scheduleDelivery(String orderId, double pickupLat, double pickupLng,
                                      double deliveryLat, double deliveryLng, double weightKg) {
        var route = new Route(pickupLat, pickupLng, deliveryLat, deliveryLng);
        var delivery = new Delivery(DeliveryId.generate(), orderId, route, weightKg);
        repository.save(delivery);
        logger.log(Level.INFO, "schedule delivery " + delivery.getId().value() + " for order " + orderId);
        observers.forEach(o -> o.notifyDeliveryScheduled(delivery.getId().value()));

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

    @Override
    public Delivery startDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.startTransit();
        repository.save(delivery);
        logger.log(Level.INFO, "start delivery " + deliveryId.value());
        return delivery;
    }

    @Override
    public void updateDronePosition(String droneId, double lat, double lng) {
        var delivery = repository.findByDroneId(droneId);
        if (delivery.isPresent()) {
            delivery.get().updatePosition(lat, lng);
            repository.save(delivery.get());
            logger.log(Level.INFO, "update position of delivery " + delivery.get().getId().value()
                + " to (" + lat + ", " + lng + ")");
        } else {
            logger.log(Level.WARNING, "no delivery found for drone " + droneId);
        }
    }

    @Override
    public Delivery completeDelivery(DeliveryId deliveryId) {
        var delivery = repository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));
        delivery.complete();
        /* read the events before saving: the repository hands them to the event store and clears them */
        var events = List.copyOf(delivery.pendingEvents());
        repository.save(delivery);
        logger.log(Level.INFO, "complete delivery " + deliveryId.value());
        observers.forEach(o -> o.notifyDeliveryCompleted(deliveryId.value()));

        events.stream()
            .filter(e -> e instanceof DeliveryCompleted)
            .map(e -> (DeliveryCompleted) e)
            .forEach(e -> {
                logger.log(Level.INFO, "notifying delivery-completed for order " + e.orderId()
                    + " and releasing drone " + e.droneId());
                orderService.notifyDeliveryCompleted(e.orderId());
                droneService.releaseDrone(e.droneId());
            });

        return delivery;
    }

    @Override
    public Optional<Delivery> trackDelivery(DeliveryId deliveryId) {
        return repository.findById(deliveryId);
    }

    @Override
    public Optional<Delivery> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId);
    }
}
