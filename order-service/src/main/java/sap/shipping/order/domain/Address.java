package sap.shipping.order.domain;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;

public class Address implements ValueObject {

    private final String street;
    private final double lat;
    private final double lng;

    public Address(String street, double lat, double lng) {
        this.street = Objects.requireNonNull(street);
        this.lat = lat;
        this.lng = lng;
    }

    public String street() { return street; }
    public double lat() { return lat; }
    public double lng() { return lng; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address a = (Address) o;
        return street.equals(a.street) && Double.compare(lat, a.lat) == 0 && Double.compare(lng, a.lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, lat, lng);
    }
}
