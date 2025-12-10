package product.shipping;

import product.PhysicalProduct;

/**
 * Calculates shipping cost for express delivery, typically at a higher rate.
 * This policy includes a base fee plus a weight-dependent component.
 */
public class ExpressShippingPolicy implements ShippingPolicy {
    private final double baseFee;
    private final double costPerKg;

    public ExpressShippingPolicy(double baseFee, double costPerKg) {
        this.baseFee = baseFee;
        this.costPerKg = costPerKg;
    }

    @Override
    public double calculateCost(PhysicalProduct product) {
        // Express shipping could also factor in volumetric weight.
        double volumetric = (product.getLengthCm() * product.getWidthCm() * product.getHeightCm()) / 5000.0;
        double billableWeight = Math.max(product.getWeightKg(), volumetric);
        
        return baseFee + (billableWeight * costPerKg);
    }

    @Override
    public String getName() {
        return "Express Shipping";
    }
}
