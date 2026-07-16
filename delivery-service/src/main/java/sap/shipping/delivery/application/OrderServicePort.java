package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.OutBoundPort;

@OutBoundPort
public interface OrderServicePort {
    void notifyDeliveryCompleted(String orderId);
}
