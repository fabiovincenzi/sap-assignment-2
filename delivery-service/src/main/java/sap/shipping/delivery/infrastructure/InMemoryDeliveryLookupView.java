package sap.shipping.delivery.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryLookupView;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Adapter
public class InMemoryDeliveryLookupView implements DeliveryLookupView {

    private final Map<String, DeliveryId> byOrderId = new ConcurrentHashMap<>();
    private final Map<String, DeliveryId> byDroneId = new ConcurrentHashMap<>();

    @Override
    public Optional<DeliveryId> findByOrderId(String orderId) {
        return Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public Optional<DeliveryId> findByDroneId(String droneId) {
        return Optional.ofNullable(byDroneId.get(droneId));
    }

    @Override
    public void indexOrder(String orderId, DeliveryId deliveryId) {
        byOrderId.put(orderId, deliveryId);
    }

    @Override
    public void indexDrone(String droneId, DeliveryId deliveryId) {
        byDroneId.put(droneId, deliveryId);
    }

    @Override
    public void forgetDrone(String droneId) {
        byDroneId.remove(droneId);
    }

    @Override
    public void clear() {
        byOrderId.clear();
        byDroneId.clear();
    }
}
