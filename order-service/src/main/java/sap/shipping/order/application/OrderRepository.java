package sap.shipping.order.application;

import sap.shipping.common.ddd.Repository;
import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.order.domain.Order;
import sap.shipping.order.domain.OrderId;
import java.util.Optional;

@OutBoundPort
public interface OrderRepository extends Repository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}
