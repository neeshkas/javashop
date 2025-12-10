package product.shipping;

import product.PhysicalProduct;

/**
 * Provides free shipping if the product's price is over a certain threshold,
 * otherwise applies a flat shipping fee.
 */
public class FreeOverThresholdShippingPolicy implements ShippingPolicy {
    private final double threshold;
    private final double flatFee;

    public FreeOverThresholdShippingPolicy(double threshold, double flatFee) {
        this.threshold = threshold;
        this.flatFee = flatFee;
    }

    @Override
    public double calculateCost(PhysicalProduct product) {
        if (product.getPrice() >= threshold) {
            return 0.0;
        }
        return flatFee;
    }

    @Override
    public String getName() {
        return "Free Shipping Over " + threshold;
    }
}
