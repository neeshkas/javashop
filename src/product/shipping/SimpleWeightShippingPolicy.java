package product.shipping;

import product.PhysicalProduct;

/**
 * Calculates shipping cost based on a simple weight-based formula.
 */
public class SimpleWeightShippingPolicy implements ShippingPolicy {
    private final double costPerKg;

    public SimpleWeightShippingPolicy(double costPerKg) {
        this.costPerKg = costPerKg;
    }

    @Override
    public double calculateCost(PhysicalProduct product) {
        return product.getWeightKg() * costPerKg;
    }

    @Override
    public String getName() {
        return "Simple Weight-Based Shipping";
    }
}
