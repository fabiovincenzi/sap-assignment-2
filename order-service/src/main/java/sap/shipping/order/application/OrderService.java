package sap.shipping.order.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.order.domain.*;
import sap.shipping.order.domain.events.OrderConfirmed;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@InBoundPort
public class OrderService {

    static Logger logger = Logger.getLogger("[Order Service]");

    private final OrderRepository repository;
    private final DeliveryServicePort deliveryService;
    private final List<OrderServiceObserver> observers = new ArrayList<>();

    public OrderService(OrderRepository repository, DeliveryServicePort deliveryService) {
        this.repository = repository;
        this.deliveryService = deliveryService;
    }

    public void addObserver(OrderServiceObserver observer) {
        observers.add(observer);
    }

    public Order createOrder(String customerId, Address pickup, Address delivery, PackageInfo packageInfo) {
        var order = new Order(OrderId.generate(), customerId, pickup, delivery, packageInfo);
        repository.save(order);
        logger.log(Level.INFO, "create new order " + order.getId().value() + " for customer " + customerId);
        observers.forEach(o -> o.notifyOrderCreated(order.getId().value()));
        return order;
    }

    public Order confirmOrder(OrderId orderId) {
        var order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.confirm();
        repository.save(order);
        logger.log(Level.INFO, "confirm order " + orderId.value());
        order.pendingEvents().stream()
            .filter(e -> e instanceof OrderConfirmed)
            .map(e -> (OrderConfirmed) e)
            .forEach(event -> {
                logger.log(Level.INFO, "notifying order-confirmed for order " + orderId.value());
                deliveryService.notifyOrderConfirmed(event);
            });
        order.clearEvents();
        return order;
    }

    public Order cancelOrder(OrderId orderId) {
        var order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.cancel();
        repository.save(order);
        logger.log(Level.INFO, "cancel order " + orderId.value());
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
