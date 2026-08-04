package delivery_service_tests.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sap.shipping.delivery.application.DeliverySnapshotStore;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliveryStatus;
import sap.shipping.delivery.domain.Route;
import sap.shipping.delivery.infrastructure.EventSourcedDeliveryRepository;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryEventStore;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryLookupView;
import sap.shipping.delivery.infrastructure.InMemoryDeliverySnapshotStore;

import static org.assertj.core.api.Assertions.*;

public class DeliverySnapshotTest {

    private static final int SNAPSHOT_EVERY = 5;
    private static final int POSITION_UPDATES = 10;

    private InMemoryDeliveryEventStore eventStore;
    private DeliverySnapshotStore snapshotStore;
    private EventSourcedDeliveryRepository repository;
    private DeliveryId deliveryId;

    @BeforeEach
    public void setUp() {
        eventStore = new InMemoryDeliveryEventStore();
        snapshotStore = new InMemoryDeliverySnapshotStore();
        repository = new EventSourcedDeliveryRepository(eventStore, snapshotStore,
            new InMemoryDeliveryLookupView(), SNAPSHOT_EVERY);
        deliveryId = DeliveryId.generate();
    }

    /** A delivery in transit that keeps reporting the drone position: 13 events in total. */
    private void aDeliveryWithManyEvents() {
        var delivery = new Delivery(deliveryId, "order-1", new Route(44.0, 12.0, 44.1, 12.1), 2.5);
        repository.save(delivery);
        delivery.assignDrone("drone-1");
        repository.save(delivery);
        delivery.startTransit();
        repository.save(delivery);
        for (int i = 1; i <= POSITION_UPDATES; i++) {
            delivery.updatePosition(44.0 + i * 0.001, 12.0);
            repository.save(delivery);
        }
    }

    @Test
    public void aSnapshotIsTakenEveryNEvents() {
        aDeliveryWithManyEvents();

        assertThat(eventStore.currentVersion(deliveryId)).isEqualTo(13);
        // taken at version 5 and then at 10, never in between
        assertThat(snapshotStore.findLatest(deliveryId)).isPresent();
        assertThat(snapshotStore.findLatest(deliveryId).get().version()).isEqualTo(10);
    }

    @Test
    public void loadingFromASnapshotOnlyReplaysTheEventsAfterIt() {
        aDeliveryWithManyEvents();

        var delivery = repository.findById(deliveryId).orElseThrow();

        assertThat(delivery.version()).isEqualTo(13);
        assertThat(delivery.status()).isEqualTo(DeliveryStatus.IN_TRANSIT);
        assertThat(delivery.droneId()).isEqualTo("drone-1");
        assertThat(delivery.currentLat()).isEqualTo(44.0 + POSITION_UPDATES * 0.001);
    }

    /**
     * The property that makes snapshots safe: they are derived data, so throwing them away
     * leaves the service correct - only slower.
     */
    @Test
    public void droppingEverySnapshotKeepsTheStateCorrect() {
        aDeliveryWithManyEvents();
        var withSnapshot = repository.findById(deliveryId).orElseThrow();

        // same events, no snapshots at all
        var withoutSnapshot = new EventSourcedDeliveryRepository(eventStore,
                new InMemoryDeliverySnapshotStore(), new InMemoryDeliveryLookupView(), SNAPSHOT_EVERY)
            .findById(deliveryId).orElseThrow();

        assertThat(withoutSnapshot.version()).isEqualTo(withSnapshot.version());
        assertThat(withoutSnapshot.status()).isEqualTo(withSnapshot.status());
        assertThat(withoutSnapshot.orderId()).isEqualTo(withSnapshot.orderId());
        assertThat(withoutSnapshot.droneId()).isEqualTo(withSnapshot.droneId());
        assertThat(withoutSnapshot.currentLat()).isEqualTo(withSnapshot.currentLat());
        assertThat(withoutSnapshot.currentLng()).isEqualTo(withSnapshot.currentLng());
        assertThat(withoutSnapshot.createdAt()).isEqualTo(withSnapshot.createdAt());
    }
}
