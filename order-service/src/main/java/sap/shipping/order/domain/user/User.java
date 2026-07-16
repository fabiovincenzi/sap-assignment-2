package sap.shipping.order.domain.user;

import sap.shipping.common.ddd.Entity;
import java.util.Objects;

public class User implements Entity<UserId> {

    private final UserId id;
    private final String username;
    private final String passwordHash;

    public User(UserId id, String username, String passwordHash) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    @Override
    public UserId getId() { return id; }

    public String username() { return username; }
    public String passwordHash() { return passwordHash; }

    public boolean checkPassword(String password) {
        return passwordHash.equals(Integer.toString(password.hashCode()));
    }

    public static String hashPassword(String password) {
        return Integer.toString(password.hashCode());
    }
}
