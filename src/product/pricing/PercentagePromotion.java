package product.pricing;

/**
 * Percentage-based promotion (e.g., 15% off).
 * Extends Promotion and implements specific discount logic.
 */
public class PercentagePromotion extends Promotion {
    private final double percent; // 0..90

    public PercentagePromotion(double percent) {
        this.percent = Math.max(0, Math.min(90, percent));
    }

    @Override
    public String name() {
        return "Percent-" + percent + "%";
    }

    @Override
    protected double calculateDiscount(double basePrice, int qty) {
        double discountedUnit = basePrice * (1 - percent / 100.0);
        return discountedUnit * qty;
    }
}
