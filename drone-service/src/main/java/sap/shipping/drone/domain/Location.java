package sap.shipping.drone.domain;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;

public class Location implements ValueObject {

    private final double lat;
    private final double lng;

    public Location(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double lat() { return lat; }
    public double lng() { return lng; }

    public double distanceTo(Location other) {
        double dlat = Math.toRadians(other.lat - this.lat);
        double dlng = Math.toRadians(other.lng - this.lng);
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
            + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(other.lat))
            * Math.sin(dlng / 2) * Math.sin(dlng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location l = (Location) o;
        return Double.compare(lat, l.lat) == 0 && Double.compare(lng, l.lng) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lng);
    }
}
