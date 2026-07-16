package sap.shipping.order.application;

import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.order.domain.user.User;
import sap.shipping.order.domain.user.UserId;

@InBoundPort
public class UserService {

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
        return user;
    }

    public User login(String username, String password) {
        var user = repository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return user;
    }
}
