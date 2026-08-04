package delivery_service_tests.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.application.DeliveryEventStore;
import sap.shipping.delivery.application.EventStoreConcurrencyException;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.DeliveryStatus;
import sap.shipping.delivery.domain.Route;
import sap.shipping.delivery.domain.events.DeliveryScheduled;
import sap.shipping.delivery.domain.events.DroneAssigned;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryEventStore;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class DeliveryEventStoreTest {

    private DeliveryEventStore eventStore;
    private DeliveryId deliveryId;

    @BeforeEach
    public void setUp() {
        eventStore = new InMemoryDeliveryEventStore();
        deliveryId = DeliveryId.generate();
    }

    private Delivery aDelivery() {
        return new Delivery(deliveryId, "order-1", new Route(44.0, 12.0, 44.1, 12.1), 2.5);
    }

    @Test
    public void unknownDeliveryHasNoEvents() {
        assertThat(eventStore.load(deliveryId)).isEmpty();
        assertThat(eventStore.currentVersion(deliveryId)).isEqualTo(DeliveryEventStore.NEW_AGGREGATE);
    }

    @Test
    public void eventsAreStoredInOrderAndBumpTheVersion() {
        var delivery = aDelivery();
        delivery.assignDrone("drone-1");

        eventStore.append(deliveryId, DeliveryEventStore.NEW_AGGREGATE, delivery.pendingEvents());

        assertThat(eventStore.load(deliveryId))
            .hasSize(2)
            .satisfiesExactly(
                first -> assertThat(first).isInstanceOf(DeliveryScheduled.class),
                second -> assertThat(second).isInstanceOf(DroneAssigned.class));
        assertThat(eventStore.currentVersion(deliveryId)).isEqualTo(2);
    }

    @Test
    public void replayingTheEventsRebuildsTheState() {
        var delivery = aDelivery();
        delivery.assignDrone("drone-1");
        delivery.startTransit();
        delivery.updatePosition(44.05, 12.05);
        delivery.complete();
        eventStore.append(deliveryId, DeliveryEventStore.NEW_AGGREGATE, delivery.pendingEvents());

        var rebuilt = new Delivery();
        eventStore.load(deliveryId).forEach(rebuilt::apply);

        assertThat(rebuilt.getId()).isEqualTo(delivery.getId());
        assertThat(rebuilt.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(rebuilt.orderId()).isEqualTo("order-1");
        assertThat(rebuilt.droneId()).isEqualTo("drone-1");
        assertThat(rebuilt.currentLat()).isEqualTo(44.05);
        assertThat(rebuilt.currentLng()).isEqualTo(12.05);
        // the creation instant travels in the event, so a replay reproduces it exactly
        assertThat(rebuilt.createdAt()).isEqualTo(delivery.createdAt());
    }

    @Test
    public void appendingFromAStaleVersionIsRejected() {
        var delivery = aDelivery();
        eventStore.append(deliveryId, DeliveryEventStore.NEW_AGGREGATE, delivery.pendingEvents());

        // another command read the delivery before that append and still believes it is at version 0
        List<DomainEvent> concurrentEvents = List.of(new DroneAssigned(deliveryId, "drone-2"));

        assertThatThrownBy(() ->
                eventStore.append(deliveryId, DeliveryEventStore.NEW_AGGREGATE, concurrentEvents))
            .isInstanceOf(EventStoreConcurrencyException.class);
        assertThat(eventStore.currentVersion(deliveryId)).isEqualTo(1);
    }

    @Test
    public void storedEventsAreNeverOverwritten() {
        var delivery = aDelivery();
        eventStore.append(deliveryId, DeliveryEventStore.NEW_AGGREGATE, delivery.pendingEvents());
        var firstEvent = eventStore.load(deliveryId).get(0);

        delivery.clearEvents();
        delivery.assignDrone("drone-1");
        eventStore.append(deliveryId, 1, delivery.pendingEvents());

        assertThat(eventStore.load(deliveryId).get(0)).isSameAs(firstEvent);
        assertThat(eventStore.currentVersion(deliveryId)).isEqualTo(2);
    }
}
