package sap.shipping.order.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.order.domain.Address;
import sap.shipping.order.domain.OrderId;
import sap.shipping.order.domain.PackageInfo;

public record OrderConfirmed(
    OrderId orderId,
    Address pickup,
    Address delivery,
    PackageInfo packageInfo
) implements DomainEvent {}
