package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliverySnapshot;
import java.util.Optional;

/**
 * Keeps the latest snapshot of each delivery. Snapshots are an optimisation only:
 * the event store stays the source of truth and can always rebuild them.
 */
@OutBoundPort
public interface DeliverySnapshotStore {

    void save(DeliverySnapshot snapshot);

    Optional<DeliverySnapshot> findLatest(DeliveryId id);
}
