package sap.shipping.order.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.OrderRepository;
import sap.shipping.order.domain.Order;
import sap.shipping.order.domain.OrderId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Adapter
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }
}
