package sap.shipping.order.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.OrderRepository;
import sap.shipping.order.domain.Order;
import sap.shipping.order.domain.OrderId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryOrderRepository implements OrderRepository {

    static Logger logger = Logger.getLogger("[OrderRepo]");

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getId(), order);
        logger.log(Level.INFO, "save order " + order.getId().value() + " - status " + order.status());
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }
}
