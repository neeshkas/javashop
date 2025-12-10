package product.pricing;

import product.Product;

/**
 * A promotion where if a customer buys three items of the same product,
 * they pay only for two.
 */
public class BuyThreePayForTwo extends Promotion {

    public BuyThreePayForTwo() {
        super();
    }

    @Override
    public String name() {
        return "Buy 3, Pay for 2";
    }

    @Override
    public boolean applicableTo(Product product) {
        // This promotion can apply to any product.
        return true;
    }

    @Override
    protected double calculateDiscount(double basePrice, int qty) {
        if (qty < 3) {
            return basePrice * qty; // No discount
        }
        // For every 3 items, one is free.
        int numberOfDiscounts = qty / 3;
        double discountAmount = numberOfDiscounts * basePrice;
        return (basePrice * qty) - discountAmount;
    }
}
