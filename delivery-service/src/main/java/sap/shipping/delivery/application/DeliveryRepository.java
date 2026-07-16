package sap.shipping.delivery.application;

import sap.shipping.common.ddd.Repository;
import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.Optional;

@OutBoundPort
public interface DeliveryRepository extends Repository {
    void save(Delivery delivery);
    Optional<Delivery> findById(DeliveryId id);
    Optional<Delivery> findByOrderId(String orderId);
    Optional<Delivery> findByDroneId(String droneId);
}
