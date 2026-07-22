package sap.shipping.gateway.application;

import java.util.Optional;

import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.gateway.domain.NewOrder;
import sap.shipping.gateway.domain.OrderView;
import sap.shipping.gateway.domain.UserView;

/**
 * What the gateway needs from the Order Service. The implementation
 * (OrderServiceProxy) talks HTTP; this interface hides that from the controller.
 */
@OutBoundPort
public interface OrderServicePort {

    UserView register(String username, String password) throws ServiceNotAvailableException;

    UserView login(String username, String password) throws ServiceNotAvailableException;

    OrderView createOrder(NewOrder order) throws ServiceNotAvailableException;

    /** Empty when the order does not exist (HTTP 404). */
    Optional<OrderView> getOrder(String orderId) throws ServiceNotAvailableException;

    OrderView confirmOrder(String orderId) throws ServiceNotAvailableException;

    OrderView cancelOrder(String orderId) throws ServiceNotAvailableException;
}
