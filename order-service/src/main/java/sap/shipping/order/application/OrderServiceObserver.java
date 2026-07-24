package sap.shipping.order.application;

import sap.shipping.common.exagonal.OutBoundPort;

@OutBoundPort
public interface OrderServiceObserver {
    void notifyOrderCreated(String orderId);
}
