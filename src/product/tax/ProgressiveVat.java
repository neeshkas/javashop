package product.tax;

import product.Product;

/**
 * A tax policy with progressive rates based on the product's subtotal price.
 * Rules:
 * - Up to 100: +5%
 * - 100–500: +10%
 * - Over 500: +15%
 */
public class ProgressiveVat implements TaxPolicy {

    @Override
    public String name() {
        return "Progressive VAT";
    }

    @Override
    public double calculateTax(Product p, double subtotal) {
        if (subtotal <= 100) {
            return subtotal * 0.05; // 5%
        } else if (subtotal <= 500) {
            return subtotal * 0.10; // 10%
        } else {
            return subtotal * 0.15; // 15%
        }
    }

    @Override
    public boolean applicableTo(Product p) {
        // This policy can apply to any product
        return true;
    }
}
