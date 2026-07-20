package sap.shipping.order.infrastructure;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.order.application.UserRepository;
import sap.shipping.order.domain.user.User;
import sap.shipping.order.domain.user.UserId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Adapter
public class InMemoryUserRepository implements UserRepository {

    static Logger logger = Logger.getLogger("[UserRepo]");

    private final Map<UserId, User> store = new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        store.put(user.getId(), user);
        logger.log(Level.INFO, "save user " + user.username());
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return store.values().stream()
            .filter(u -> u.username().equals(username))
            .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return store.values().stream()
            .anyMatch(u -> u.username().equals(username));
    }
}
