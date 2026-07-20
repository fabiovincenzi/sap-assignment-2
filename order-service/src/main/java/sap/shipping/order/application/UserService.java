package sap.shipping.order.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.order.domain.user.User;
import sap.shipping.order.domain.user.UserId;
import java.util.logging.Level;
import java.util.logging.Logger;

@InBoundPort
public class UserService {

    static Logger logger = Logger.getLogger("[User Service]");

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User register(String username, String password) {
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        var user = new User(UserId.generate(), username, User.hashPassword(password));
        repository.save(user);
        logger.log(Level.INFO, "register new user " + username);
        return user;
    }

    public User login(String username, String password) {
        var user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        logger.log(Level.INFO, "login user " + username);
        return user;
    }
}
