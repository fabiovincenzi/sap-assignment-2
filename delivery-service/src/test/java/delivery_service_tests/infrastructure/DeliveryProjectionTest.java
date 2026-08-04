package delivery_service_tests.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sap.shipping.delivery.application.DeliveryLookupView;
import sap.shipping.delivery.domain.Delivery;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.Route;
import sap.shipping.delivery.infrastructure.EventSourcedDeliveryRepository;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryEventStore;
import sap.shipping.delivery.infrastructure.InMemoryDeliveryLookupView;
import sap.shipping.delivery.infrastructure.InMemoryDeliverySnapshotStore;

import static org.assertj.core.api.Assertions.*;

public class DeliveryProjectionTest {

    private DeliveryLookupView lookupView;
    private EventSourcedDeliveryRepository repository;
    private DeliveryId deliveryId;

    @BeforeEach
    public void setUp() {
        lookupView = new InMemoryDeliveryLookupView();
        repository = new EventSourcedDeliveryRepository(new InMemoryDeliveryEventStore(),
            new InMemoryDeliverySnapshotStore(), lookupView);
        deliveryId = DeliveryId.generate();

        var delivery = new Delivery(deliveryId, "order-1", new Route(44.0, 12.0, 44.1, 12.1), 2.5);
        repository.save(delivery);
        delivery.assignDrone("drone-1");
        repository.save(delivery);
    }

    @Test
    public void theViewIsFedByTheEvents() {
        assertThat(lookupView.findByOrderId("order-1")).contains(deliveryId);
        assertThat(lookupView.findByDroneId("drone-1")).contains(deliveryId);
        assertThat(repository.findByOrderId("order-1")).isPresent();
        assertThat(repository.findByDroneId("drone-1")).isPresent();
    }

    @Test
    public void completingADeliveryFreesTheDroneInTheView() {
        var delivery = repository.findById(deliveryId).orElseThrow();
        delivery.startTransit();
        delivery.complete();
        repository.save(delivery);

        assertThat(lookupView.findByDroneId("drone-1")).isEmpty();
        assertThat(lookupView.findByOrderId("order-1")).contains(deliveryId);
    }

    /**
     * The property that makes the read model safe: it is derived data, so it can be dropped
     * and rebuilt from the events without losing anything.
     */
    @Test
    public void theViewCanBeDroppedAndRebuiltFromTheEvents() {
        lookupView.clear();
        assertThat(lookupView.findByOrderId("order-1")).isEmpty();
        assertThat(lookupView.findByDroneId("drone-1")).isEmpty();

        repository.rebuildLookupView();

        assertThat(lookupView.findByOrderId("order-1")).contains(deliveryId);
        assertThat(lookupView.findByDroneId("drone-1")).contains(deliveryId);
    }
}
