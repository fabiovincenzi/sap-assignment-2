package sap.shipping.order.application;

import sap.shipping.common.ddd.Repository;
import sap.shipping.common.exagonal.OutBoundPort;
import sap.shipping.order.domain.user.User;
import sap.shipping.order.domain.user.UserId;
import java.util.Optional;

@OutBoundPort
public interface UserRepository extends Repository {
    void save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
