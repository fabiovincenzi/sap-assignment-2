package sap.shipping.gateway.application;

import java.util.Optional;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.gateway.domain.DeliveryView;

/**
 * What the gateway needs from the Delivery Service. Used by the tracking
 * aggregation to fetch the delivery associated with an order.
 */
@OutBoundPort
public interface DeliveryServicePort {

    /** Empty when the order has no delivery yet (HTTP 404). */
    Optional<DeliveryView> findByOrderId(String orderId) throws ServiceNotAvailableException;
}
