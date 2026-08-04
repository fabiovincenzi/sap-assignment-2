package delivery_service_tests.infrastructure;

import sap.shipping.delivery.application.DeliveryService;
import sap.shipping.delivery.application.DeliveryServiceObserver;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.Route;
import java.util.Optional;

/**
 * Canned replies for the service, so that the controller can be tested on its own:
 * routing, payload parsing and status codes, with no business logic behind.
 */
public class DeliveryServiceMock implements DeliveryService {

    public static final String KNOWN_DELIVERY = "delivery-1";
    public static final String KNOWN_ORDER = "order-1";

    public String lastDroneId;
    public double lastLat;
    public double lastLng;

    private Delivery aDelivery(String id) {
        var delivery = new Delivery(new DeliveryId(id), KNOWN_ORDER, new Route(44.0, 12.0, 44.1, 12.1), 2.5);
        delivery.clearEvents();
        return delivery;
    }

    @Override
    public void addObserver(DeliveryServiceObserver observer) {
    }

    @Override
    public Delivery scheduleDelivery(String orderId, double pickupLat, double pickupLng,
                                     double deliveryLat, double deliveryLng, double weightKg) {
        return aDelivery(KNOWN_DELIVERY);
    }

    @Override
    public Delivery startDelivery(DeliveryId deliveryId) {
        return aDelivery(deliveryId.value());
    }

    @Override
    public void updateDronePosition(String droneId, double lat, double lng) {
        lastDroneId = droneId;
        lastLat = lat;
        lastLng = lng;
    }

    @Override
    public Delivery completeDelivery(DeliveryId deliveryId) {
        return aDelivery(deliveryId.value());
    }

    @Override
    public Optional<Delivery> trackDelivery(DeliveryId deliveryId) {
        return KNOWN_DELIVERY.equals(deliveryId.value()) ? Optional.of(aDelivery(KNOWN_DELIVERY)) : Optional.empty();
    }

    @Override
    public Optional<Delivery> findByOrderId(String orderId) {
        return KNOWN_ORDER.equals(orderId) ? Optional.of(aDelivery(KNOWN_DELIVERY)) : Optional.empty();
    }
}
