package product.shipping;

/**
 * Interface for products that require physical shipping.
 * Physical products should implement this interface.
 */
public interface Shippable {
    /**
     * Calculate the shipping cost for this product.
     * @return the shipping cost in the product's currency
     */
    double getShippingCost();

    /**
     * Check if this product requires shipping.
     * @return true if shipping is required, false otherwise
     */
    default boolean requiresShipping() {
        return true;
    }
}
