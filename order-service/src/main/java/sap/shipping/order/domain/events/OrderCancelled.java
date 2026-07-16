package sap.shipping.order.domain.events;

import sap.shipping.common.ddd.DomainEvent;
import sap.shipping.order.domain.OrderId;

public record OrderCancelled(OrderId orderId) implements DomainEvent {}
