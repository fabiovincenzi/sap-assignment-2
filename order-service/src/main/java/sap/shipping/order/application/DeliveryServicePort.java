package sap.shipping.order.application;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.order.domain.events.OrderConfirmed;

@OutBoundPort
public interface DeliveryServicePort {
    void notifyOrderConfirmed(OrderConfirmed event);
}
