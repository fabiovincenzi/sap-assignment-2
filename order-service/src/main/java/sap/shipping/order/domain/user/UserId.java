package sap.shipping.order.domain.user;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;
import java.util.UUID;

public class UserId implements ValueObject {

    private final String id;

    public UserId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID().toString());
    }

    public String value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((UserId) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
