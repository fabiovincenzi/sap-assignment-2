package sap.shipping.order.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.order.domain.*;
import sap.shipping.order.domain.events.OrderConfirmed;
import java.util.Optional;

@InBoundPort
public class OrderService {

    private final OrderRepository repository;
    private final DeliveryServicePort deliveryService;

    public OrderService(OrderRepository repository, DeliveryServicePort deliveryService) {
        this.repository = repository;
        this.deliveryService = deliveryService;
    }

    public Order createOrder(String customerId, Address pickup, Address delivery, PackageInfo packageInfo) {
        var order = new Order(OrderId.generate(), customerId, pickup, delivery, packageInfo);
        repository.save(order);
        return order;
    }

    public Order confirmOrder(OrderId orderId) {
        var order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.confirm();
        repository.save(order);
        order.pendingEvents().stream()
            .filter(e -> e instanceof OrderConfirmed)
            .map(e -> (OrderConfirmed) e)
            .forEach(deliveryService::notifyOrderConfirmed);
        order.clearEvents();
        return order;
    }

    public Order cancelOrder(OrderId orderId) {
        var order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancel();
        repository.save(order);
        order.clearEvents();
        return order;
    }

    public void completeOrder(OrderId orderId) {
        var order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.complete();
        repository.save(order);
    }

    public Optional<Order> getOrder(OrderId orderId) {
        return repository.findById(orderId);
    }
}
