package product.tax;

import product.Product;
import product.DigitalProduct;

/**
 * Reduced VAT policy specifically for digital products.
 * Example: 5% VAT for digital goods vs 12% for physical.
 */
public class ReducedDigitalVat implements TaxPolicy {
    private final double vatRate; // e.g., 0.05 for 5%

    public ReducedDigitalVat(double vatRate) {
        this.vatRate = Math.max(0.0, Math.min(1.0, vatRate));
    }

    @Override
    public String name() {
        return "Reduced Digital VAT " + (int)(vatRate * 100) + "%";
    }

    @Override
    public double calculateTax(Product p, double subtotal) {
        if (applicableTo(p)) {
            return subtotal * vatRate;
        }
        return 0.0;
    }

    @Override
    public boolean applicableTo(Product p) {
        return p instanceof DigitalProduct;
    }
}
