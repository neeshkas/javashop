package product.pricing;

/**
 * Buy-One-Get-One-Half promotion.
 * For every pair: second item is -50%.
 * Price for a pair = 1.5 * price; average = 0.75 * price per unit.
 * Extends Promotion and implements specific discount logic.
 */
public class BogoHalfPromotion extends Promotion {

    @Override
    public String name() {
        return "BOGO-HALF";
    }

    @Override
    protected double calculateDiscount(double basePrice, int qty) {
        int pairs = qty / 2;
        int singles = qty % 2;
        return pairs * (basePrice * 1.5) + singles * basePrice;
    }
}
