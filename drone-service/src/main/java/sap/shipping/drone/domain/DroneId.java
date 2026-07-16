package sap.shipping.drone.domain;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;
import java.util.UUID;

public class DroneId implements ValueObject {

    private final String id;

    public DroneId(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public static DroneId generate() {
        return new DroneId(UUID.randomUUID().toString());
    }

    public String value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id.equals(((DroneId) o).id);
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
