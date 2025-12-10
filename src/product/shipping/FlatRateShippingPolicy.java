package product.shipping;

import product.PhysicalProduct;

/**
 * A shipping policy that applies a single, flat rate fee for any product,
 * regardless of its weight, dimensions, or price.
 */
public class FlatRateShippingPolicy implements ShippingPolicy {
    private final double flatRate;

    public FlatRateShippingPolicy(double flatRate) {
        this.flatRate = flatRate;
    }

    @Override
    public double calculateCost(PhysicalProduct product) {
        // The cost is always the flat rate.
        return this.flatRate;
    }

    @Override
    public String getName() {
        return "Flat Rate Shipping";
    }
}
