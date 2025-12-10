package product.pricing;

import product.Product;

public class PercentageOff implements PricePolicy {
    private final double percent; // 0..90

    public PercentageOff(double percent) {
        this.percent = Math.max(0, Math.min(90, percent));
    }

    @Override
    public String name() {
        return "Percent-" + percent + "%";
    }

    @Override
    public double apply(Product p, int qty) {
        double unit = p.getPrice() * (1 - percent / 100.0);
        return unit * Math.max(0, qty);
    }
}
