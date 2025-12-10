package product.pricing;

import product.Product;

public class FixedOff implements PricePolicy {
    private final double amount; // >= 0

    public FixedOff(double amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public String name() {
        return "Fixed-" + amount;
    }

    @Override
    public double apply(Product p, int qty) {
        double unit = Math.max(0.0, p.getPrice() - amount);
        return unit * Math.max(0, qty);
    }
}
