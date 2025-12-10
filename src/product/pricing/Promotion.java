package product.pricing;

import product.Product;

/**
 * Abstract base class for all promotions using Template Method pattern.
 * Implements PricePolicy and provides common logic for applying discounts.
 * Subclasses must define how to calculate the specific discount.
 */
public abstract class Promotion implements PricePolicy {

    /**
     * Template Method: applies promotion to product.
     * Common algorithm:
     * 1. Validate quantity
     * 2. Get base price
     * 3. Calculate discount (delegated to subclass)
     * 4. Return final price
     */
    @Override
    public final double apply(Product p, int qty) {
        if (qty <= 0) {
            return 0.0;
        }

        double basePrice = p.getPrice();
        double totalPrice = calculateDiscount(basePrice, qty);

        return Math.max(0.0, totalPrice);
    }

    /**
     * Hook method: subclasses override this to provide specific discount logic.
     * @param basePrice the unit price of the product
     * @param qty the quantity being purchased
     * @return the total price after applying the discount
     */
    protected abstract double calculateDiscount(double basePrice, int qty);

    /**
     * By default, promotions are applicable to all products.
     * Subclasses can override to restrict applicability.
     */
    @Override
    public boolean applicableTo(Product p) {
        return true;
    }
}
