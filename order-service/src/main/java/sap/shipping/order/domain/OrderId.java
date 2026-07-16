package sap.shipping.order.domain;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;
import java.util.UUID;

public class OrderId implements ValueObject {

    private final String id;

    public OrderId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public String value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((OrderId) o).id);
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
