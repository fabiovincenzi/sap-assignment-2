package sap.shipping.delivery.infrastructure;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.common.exagonal.Adapter;
import sap.shipping.delivery.application.DeliveryEventStore;
import sap.shipping.delivery.application.DeliveryRepository;
import sap.shipping.delivery.application.DeliverySnapshotStore;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliverySnapshot;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import sap.shipping.delivery.domain.events.DeliveryScheduled;
import sap.shipping.delivery.domain.events.DroneAssigned;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private final int snapshotEvery;
    private final Map<String, DeliveryId> byOrderId = new ConcurrentHashMap<>();
    private final Map<String, DeliveryId> byDroneId = new ConcurrentHashMap<>();

    public EventSourcedDeliveryRepository(DeliveryEventStore eventStore, DeliverySnapshotStore snapshotStore) {
        this(eventStore, snapshotStore, DEFAULT_SNAPSHOT_EVERY);
    }

    public EventSourcedDeliveryRepository(DeliveryEventStore eventStore, DeliverySnapshotStore snapshotStore,
                                          int snapshotEvery) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.snapshotEvery = snapshotEvery;
    }

    @Override
    public void save(Delivery delivery) {
        var newEvents = List.copyOf(delivery.pendingEvents());
        if (newEvents.isEmpty()) {
            return;
        }
        eventStore.append(delivery.getId(), delivery.persistedVersion(), newEvents);
        delivery.clearEvents();
        newEvents.forEach(event -> project(delivery.getId(), event));
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
        return Optional.ofNullable(byOrderId.get(orderId)).flatMap(this::findById);
    }

    @Override
    public Optional<Delivery> findByDroneId(String droneId) {
        return Optional.ofNullable(byDroneId.get(droneId)).flatMap(this::findById);
    }

    /** Keeps the lookup read models aligned with the events being stored. */
    private void project(DeliveryId id, DomainEvent event) {
        if (event instanceof DeliveryScheduled e) {
            byOrderId.put(e.orderId(), id);
        } else if (event instanceof DroneAssigned e) {
            byDroneId.put(e.droneId(), id);
        } else if (event instanceof DeliveryCompleted e) {
            byDroneId.remove(e.droneId());
        }
    }
}
