package delivery_service_tests.application;

import org.junit.jupiter.api.Test;
import sap.shipping.delivery.application.*;
import sap.shipping.delivery.infrastructure.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

public class DeliveryServiceTest {
    @Test
    public void completionStillNotifiesOrderAndReleasesDrone() {
        List<String> notified = new ArrayList<>();
        List<String> released = new ArrayList<>();
        var repo = new EventSourcedDeliveryRepository(new InMemoryDeliveryEventStore(), new InMemoryDeliverySnapshotStore(), new InMemoryDeliveryLookupView());
        DroneServicePort drone = new DroneServicePort() {
            public Optional<String> requestAvailableDrone(double a, double b, double c) { return Optional.of("drone-1"); }
            public void releaseDrone(String id) { released.add(id); }
        };
        OrderServicePort order = notified::add;
        var service = new DeliveryServiceImpl(repo, drone, order);

        var d = service.scheduleDelivery("order-9", 44.0, 12.0, 44.1, 12.1, 2.0);
        service.startDelivery(d.getId());
        service.completeDelivery(d.getId());

        assertThat(notified).containsExactly("order-9");
        assertThat(released).containsExactly("drone-1");
    }
}
