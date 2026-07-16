package sap.shipping.order.domain;

import sap.shipping.common.ddd.Aggregate;
import sap.shipping.order.domain.events.OrderCancelled;
import sap.shipping.order.domain.events.OrderConfirmed;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order implements Aggregate<OrderId> {

    private final OrderId id;
    private final String customerId;
    private final Address pickup;
    private final Address delivery;
    private final PackageInfo packageInfo;
    private final Instant createdAt;
    private OrderStatus status;
    private final List<Object> pendingEvents = new ArrayList<>();

    public Order(OrderId id, String customerId, Address pickup, Address delivery, PackageInfo packageInfo) {
        this.id = id;
        this.customerId = customerId;
        this.pickup = pickup;
        this.delivery = delivery;
        this.packageInfo = packageInfo;
        this.createdAt = Instant.now();
        this.status = OrderStatus.PENDING;
    }

    @Override
    public OrderId getId() { return id; }

    public String customerId() { return customerId; }
    public Address pickup() { return pickup; }
    public Address delivery() { return delivery; }
    public PackageInfo packageInfo() { return packageInfo; }
    public OrderStatus status() { return status; }
    public Instant createdAt() { return createdAt; }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only confirm a PENDING order");
        }
        this.status = OrderStatus.CONFIRMED;
        pendingEvents.add(new OrderConfirmed(id, pickup, delivery, packageInfo));
    }

    public void cancel() {
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel an order in " + status + " status");
        }
        this.status = OrderStatus.CANCELLED;
        pendingEvents.add(new OrderCancelled(id));
    }

    public void complete() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Can only complete a CONFIRMED order");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public List<Object> pendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearEvents() {
        pendingEvents.clear();
    }
}
