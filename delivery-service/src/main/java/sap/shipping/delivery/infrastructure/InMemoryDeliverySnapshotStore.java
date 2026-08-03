package sap.shipping.delivery.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliverySnapshotStore;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliverySnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryDeliverySnapshotStore implements DeliverySnapshotStore {

    static Logger logger = Logger.getLogger("[DeliverySnapshots]");

    private final Map<DeliveryId, DeliverySnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void save(DeliverySnapshot snapshot) {
        snapshots.put(snapshot.deliveryId(), snapshot);
        logger.log(Level.INFO, "snapshot delivery " + snapshot.deliveryId().value()
            + " at version " + snapshot.version());
    }

    @Override
    public Optional<DeliverySnapshot> findLatest(DeliveryId id) {
        return Optional.ofNullable(snapshots.get(id));
    }
}
