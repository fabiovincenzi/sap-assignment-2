package sap.shipping.delivery.application;

import sap.shipping.common.exagonal.OutBoundPort;

@OutBoundPort
public interface DeliveryServiceObserver {
    void notifyDeliveryScheduled(String deliveryId);
    void notifyDeliveryCompleted(String deliveryId);
}
