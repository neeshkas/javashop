package product.pricing;

import product.Product;

public interface PricePolicy {
    /**
     * Human-readable rule name (for printing).
     */
    String name();

    /**
     * Calculate the FINAL cost for 'qty' units of 'p' (NO shipping here).
     * Must NOT mutate Product.
     */
    double apply(Product p, int qty);

    /**
     * By default, applicable to all products.
     */
    default boolean applicableTo(Product p) {
        return true;
    }
}
