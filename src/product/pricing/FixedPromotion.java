package product.pricing;

/**
 * Fixed amount off promotion (e.g., 500 KZT off per unit).
 * Extends Promotion and implements specific discount logic.
 */
public class FixedPromotion extends Promotion {
    private final double amount; // >= 0

    public FixedPromotion(double amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public String name() {
        return "Fixed-" + amount;
    }

    @Override
    protected double calculateDiscount(double basePrice, int qty) {
        double discountedUnit = Math.max(0.0, basePrice - amount);
        return discountedUnit * qty;
    }
}
