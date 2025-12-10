package product.shipping;

import product.PhysicalProduct;

/**
 * An interface for defining different strategies for calculating shipping costs.
 * This allows for adding new shipping methods without modifying the product classes,
 * adhering to the Open/Closed Principle.
 */
public interface ShippingPolicy {
    /**
     * Calculates the shipping cost for a given physical product.
     *
     * @param product The product for which to calculate shipping cost.
     * @return The calculated shipping cost.
     */
    double calculateCost(PhysicalProduct product);

    /**
     * A human-readable name for the shipping policy.
     * @return The name of the policy.
     */
    String getName();
}
