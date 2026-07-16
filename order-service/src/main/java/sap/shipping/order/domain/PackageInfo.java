package sap.shipping.order.domain;

import sap.shipping.common.ddd.ValueObject;
import java.util.Objects;

public class PackageInfo implements ValueObject {

    private static final double MAX_WEIGHT_KG = 25.0;

    private final double weightKg;

    public PackageInfo(double weightKg) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (weightKg > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException("Weight exceeds maximum of " + MAX_WEIGHT_KG + " kg");
        }
        this.weightKg = weightKg;
    }

    public double weightKg() { return weightKg; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Double.compare(weightKg, ((PackageInfo) o).weightKg) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(weightKg);
    }
}
