package sap.shipping.delivery.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryEventStore;
import sap.shipping.delivery.application.DeliveryLookupView;
import sap.shipping.delivery.application.DeliveryProjector;
import sap.shipping.delivery.application.DeliveryRepository;
import sap.shipping.delivery.application.DeliverySnapshotStore;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliverySnapshot;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repository backed by the event store: a delivery is saved by appending its new events and
 * loaded by replaying them.
 *
 * The event store only answers by delivery id, so lookups by order or by drone are served by
 * read models kept up to date by the same events (CQRS).
 */
@Adapter
public class EventSourcedDeliveryRepository implements DeliveryRepository {

    static Logger logger = Logger.getLogger("[DeliveryRepo]");

    /** Events between two snapshots of the same delivery. */
    private static final int DEFAULT_SNAPSHOT_EVERY = 50;

    private final DeliveryEventStore eventStore;
    private final DeliverySnapshotStore snapshotStore;
    private final DeliveryLookupView lookupView;
    private final DeliveryProjector projector;
    private final int snapshotEvery;

    public EventSourcedDeliveryRepository(DeliveryEventStore eventStore, DeliverySnapshotStore snapshotStore,
                                          DeliveryLookupView lookupView) {
        this(eventStore, snapshotStore, lookupView, DEFAULT_SNAPSHOT_EVERY);
    }

    public EventSourcedDeliveryRepository(DeliveryEventStore eventStore, DeliverySnapshotStore snapshotStore,
                                          DeliveryLookupView lookupView, int snapshotEvery) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.lookupView = lookupView;
        this.projector = new DeliveryProjector(lookupView);
        this.snapshotEvery = snapshotEvery;
    }

    /** Drops the read model and rebuilds it from the events. */
    public void rebuildLookupView() {
        projector.rebuild(eventStore);
    }

    @Override
    public void save(Delivery delivery) {
        var newEvents = List.copyOf(delivery.pendingEvents());
        if (newEvents.isEmpty()) {
            return;
        }
        eventStore.append(delivery.getId(), delivery.persistedVersion(), newEvents);
        delivery.clearEvents();
        newEvents.forEach(event -> projector.project(delivery.getId(), event));
        takeSnapshotIfDue(delivery);
        logger.log(Level.INFO, "save delivery " + delivery.getId().value() + " - status " + delivery.status());
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        var snapshot = snapshotStore.findLatest(id);
        if (snapshot.isPresent()) {
            var delivery = Delivery.fromSnapshot(snapshot.get());
            eventStore.loadFrom(id, snapshot.get().version()).forEach(delivery::apply);
            return Optional.of(delivery);
        }
        var events = eventStore.load(id);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        var delivery = new Delivery();
        events.forEach(delivery::apply);
        return Optional.of(delivery);
    }

    private void takeSnapshotIfDue(Delivery delivery) {
        long lastSnapshotVersion = snapshotStore.findLatest(delivery.getId())
            .map(DeliverySnapshot::version)
            .orElse(0L);
        if (delivery.version() - lastSnapshotVersion >= snapshotEvery) {
            snapshotStore.save(delivery.snapshot());
        }
    }

    @Override
    public Optional<Delivery> findByOrderId(String orderId) {
        return lookupView.findByOrderId(orderId).flatMap(this::findById);
    }

    @Override
    public Optional<Delivery> findByDroneId(String droneId) {
        return lookupView.findByDroneId(droneId).flatMap(this::findById);
    }
}
