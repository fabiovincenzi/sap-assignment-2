package sap.shipping.delivery.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryRepository;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryDeliveryRepository implements DeliveryRepository {

    static Logger logger = Logger.getLogger("[DeliveryRepo]");

    private final Map<DeliveryId, Delivery> store = new ConcurrentHashMap<>();

    @Override
    public void save(Delivery delivery) {
        store.put(delivery.getId(), delivery);
        logger.log(Level.INFO, "save delivery " + delivery.getId().value() + " - status " + delivery.status());
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Delivery> findByOrderId(String orderId) {
        return store.values().stream()
            .filter(d -> d.orderId().equals(orderId))
            .findFirst();
    }

    @Override
    public Optional<Delivery> findByDroneId(String droneId) {
        return store.values().stream()
            .filter(d -> droneId.equals(d.droneId()))
            .findFirst();
    }
}
