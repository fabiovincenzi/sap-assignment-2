package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.Optional;

/** Inbound port of the delivery service: what the outside world can ask it to do. */
@InBoundPort
public interface DeliveryService {

    void addObserver(DeliveryServiceObserver observer);

    Delivery scheduleDelivery(String orderId, double pickupLat, double pickupLng,
                              double deliveryLat, double deliveryLng, double weightKg);

    Delivery startDelivery(DeliveryId deliveryId);

    void updateDronePosition(String droneId, double lat, double lng);

    Delivery completeDelivery(DeliveryId deliveryId);

    Optional<Delivery> trackDelivery(DeliveryId deliveryId);

    Optional<Delivery> findByOrderId(String orderId);
}
