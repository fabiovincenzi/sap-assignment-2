package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.delivery.domain.DeliveryId;
import java.util.Optional;

/**
 * Read model answering the lookups the event store cannot serve, since it only retrieves a
 * delivery by its own id (CQRS).
 *
 * It holds derived data only: it can be dropped and rebuilt from the events at any time.
 */
@OutBoundPort
public interface DeliveryLookupView {

    Optional<DeliveryId> findByOrderId(String orderId);

    Optional<DeliveryId> findByDroneId(String droneId);

    void indexOrder(String orderId, DeliveryId deliveryId);

    void indexDrone(String droneId, DeliveryId deliveryId);

    void forgetDrone(String droneId);

    /** Drops the whole view, before rebuilding it from the events. */
    void clear();
}
