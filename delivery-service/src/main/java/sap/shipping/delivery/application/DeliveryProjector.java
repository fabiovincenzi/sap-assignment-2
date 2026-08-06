package sap.shipping.delivery.application;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.delivery.domain.DeliveryId;
import sap.shipping.delivery.domain.events.DeliveryCompleted;
import sap.shipping.delivery.domain.events.DeliveryScheduled;
import sap.shipping.delivery.domain.events.DroneAssigned;

/**
 * Translates domain events into updates of the read model. It is the only component allowed
 * to write on the view.
 */
public class DeliveryProjector {

    private final DeliveryLookupView view;

    public DeliveryProjector(DeliveryLookupView view) {
        this.view = view;
    }

    public void project(DeliveryId deliveryId, DomainEvent event) {
        if (event instanceof DeliveryScheduled e) {
            view.indexOrder(e.orderId(), deliveryId);
        } else if (event instanceof DroneAssigned e) {
            view.indexDrone(e.droneId(), deliveryId);
        } else if (event instanceof DeliveryCompleted e) {
            view.forgetDrone(e.droneId());
        }
        /* other events do not affect the lookups */
    }

    /** Rebuilds the view from scratch, replaying every stored event. */
    public void rebuild(DeliveryEventStore eventStore) {
        view.clear();
        eventStore.streamIds().forEach(id -> eventStore.load(id).forEach(event -> project(id, event)));
    }
}
