package product.pricing;

import product.Product;

/**
 * Buy-One-Get-One-Half pricing policy.
 * For every pair: second item is -50%.
 * Price for a pair = 1.5 * price; average = 0.75 * price per unit.
 */
public class BogoHalf implements PricePolicy {
    @Override
    public String name() {
        return "BOGO-HALF";
    }

    @Override
    public double apply(Product p, int qty) {
        double price = p.getPrice();
        int pairs = Math.max(0, qty) / 2;
        int singles = Math.max(0, qty) % 2;
        return pairs * (price * 1.5) + singles * price;
    }
}
