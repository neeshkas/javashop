package product.tax;

import product.Product;

/**
 * Tax policy with no tax applied (0%).
 */
public class NoTax implements TaxPolicy {

    @Override
    public String name() {
        return "No Tax";
    }

    @Override
    public double calculateTax(Product p, double subtotal) {
        return 0.0;
    }

    @Override
    public boolean applicableTo(Product p) {
        return true;
    }
}
