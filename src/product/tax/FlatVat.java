package product.tax;

import product.Product;

/**
 * Flat VAT (Value Added Tax) policy that applies a fixed percentage to all products.
 * Example: 12% VAT for physical goods.
 */
public class FlatVat implements TaxPolicy {
    private final double vatRate; // e.g., 0.12 for 12%

    public FlatVat(double vatRate) {
        this.vatRate = Math.max(0.0, Math.min(1.0, vatRate));
    }

    @Override
    public String name() {
        return "Flat VAT " + (int)(vatRate * 100) + "%";
    }

    @Override
    public double calculateTax(Product p, double subtotal) {
        return subtotal * vatRate;
    }

    @Override
    public boolean applicableTo(Product p) {
        return true; // applies to all products
    }
}
