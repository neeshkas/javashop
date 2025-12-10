package product.tax;

import product.Product;

/**
 * Interface for calculating taxes on products.
 * Different tax policies can be applied to different product types.
 */
public interface TaxPolicy {
    /**
     * Human-readable tax policy name.
     */
    String name();

    /**
     * Calculate tax amount for a given product and subtotal.
     * @param p the product
     * @param subtotal the pre-tax price
     * @return the tax amount to add
     */
    double calculateTax(Product p, double subtotal);

    /**
     * Check if this tax policy applies to the given product.
     * @param p the product
     * @return true if this tax applies, false otherwise
     */
    default boolean applicableTo(Product p) {
        return true;
    }
}
