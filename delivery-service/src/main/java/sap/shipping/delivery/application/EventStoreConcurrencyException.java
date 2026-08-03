package sap.shipping.delivery.application;

import sap.shipping.delivery.domain.DeliveryId;

/** Raised when a delivery has been modified between the read and the append. */
public class EventStoreConcurrencyException extends RuntimeException {

    public EventStoreConcurrencyException(DeliveryId id, long expectedVersion, long actualVersion) {
        super("Delivery " + id.value() + " expected at version " + expectedVersion
            + " but is at version " + actualVersion);
    }
}
