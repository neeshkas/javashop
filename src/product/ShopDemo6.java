package product;

import product.pricing.*;
import product.pricing.PricePolicy;
import product.shipping.*;
import product.tax.*;

import java.util.List;

/**
 * Requirement 8 Demo: OCP Extension
 *
 * Demonstrates:
 * 1. Adding ProgressiveVat without changing existing tax code.
 * 2. Refactoring shipping logic using ShippingPolicy interface.
 * 3. Adding new shipping methods without changing PhysicalProduct.
 */
public class ShopDemo6 {

    public static void main(String[] args) {
        System.out.println("=== REQUIREMENT 8: OCP Extension Demo ===\n");

        // --- Create Policies ---
        
        // Promotions (now a list of PricePolicy)
        List<PricePolicy> promotions = List.of(
            new PercentagePromotion(15),    // 15% off
            new FixedPromotion(10000),      // 10000 KZT off
            new BogoHalfPromotion(),         // Buy one get second at 50% off
            new BuyThreePayForTwo()
        );

        // Tax Policies
        TaxPolicy noTax = new NoTax();
        TaxPolicy flatVat = new FlatVat(0.12);
        TaxPolicy reducedVat = new ReducedDigitalVat(0.05);
        TaxPolicy progressiveVat = new ProgressiveVat(); // New!

        // Shipping Policies (New!)
        ShippingPolicy simpleShipping = new SimpleWeightShippingPolicy(1500); // 1500 KZT per kg
        ShippingPolicy expressShipping = new ExpressShippingPolicy(5000, 2500); // 5000 base, 2500/kg
        ShippingPolicy freeShipping = new FreeOverThresholdShippingPolicy(100000, 4000); // Free over 100k

        // --- Create Products ---
        // PhysicalProducts now require a ShippingPolicy
        PhysicalProduct laptop = new PhysicalProduct(
            "LP001", "Gaming Laptop", "High-performance gaming laptop",
            150000.0, 5, 2.5, 40.0, 30.0, 5.0, expressShipping // Using express
        );

        DigitalProduct ebook = new DigitalProduct(
            "EB001", "Java Programming Guide", "Comprehensive Java tutorial",
            5000.0, 100, 25.5, "LICENSE-2024-JAVA"
        );

        PhysicalProduct headphones = new PhysicalProduct(
            "HP001", "Wireless Headphones", 25000.0, 0.3, simpleShipping // Using simple
        );

        System.out.println("=== PRODUCTS & POLICIES INITIALIZED ===\n");

        // --- DEMONSTRATIONS ---
        
        // Demo 1: The new ProgressiveVat tax policy in action
        demonstrateProgressiveVat(progressiveVat);

        // Demo 2: The new ShippingPolicy system
        demonstrateShippingPolicies(laptop, simpleShipping, freeShipping);

        // Demo 3: Full price calculation with new policies
        System.out.println("\n=== DEMO 3: Full Price Calculation (Laptop, Qty: 2, Tax: Progressive VAT) ===");
        demonstrateFullPrice(laptop, 2, promotions, progressiveVat);
        
        // Demo 4: Full Price Calculation (Headphones, Qty: 4, Tax: Flat VAT, Promo: BOGO) ===
        demonstrateFullPrice(headphones, 4, List.of(new BogoHalfPromotion()), flatVat);

        // --- OCP VERIFICATION ---
        System.out.println("\n=== DEMO 5: Verifying OCP - Adding New Shipping without changing Product ===");
        demonstrateOcpExtension(headphones);
    }

    /**
     * DEMO 5: This method proves the Open/Closed Principle.
     * We can introduce a completely new shipping policy and apply it without
     * ever touching the source code of the PhysicalProduct class.
     */
    private static void demonstrateOcpExtension(PhysicalProduct product) {
        System.out.println("Product: " + product.getName());
        System.out.printf("Base price: %.2f KZT\n", product.getPrice());
        System.out.println("Initial shipping policy: " + product.getShippingPolicy().getName());
        double initialShippingCost = product.getShippingCost();
        System.out.printf("Initial shipping cost: %.2f KZT\n", initialShippingCost);
        System.out.printf("Initial total price (product + shipping): %.2f KZT\n", product.getPrice() + initialShippingCost);

        // Now, create and apply the *new* policy that we just added.
        ShippingPolicy flatRatePolicy = new FlatRateShippingPolicy(2000); // 2000 KZT flat rate
        
        System.out.println("\nApplying newly created 'FlatRateShippingPolicy'...");
        product.setShippingPolicy(flatRatePolicy);

        System.out.println("Current shipping policy: " + product.getShippingPolicy().getName());
        double newShippingCost = product.getShippingCost();
        System.out.printf("New shipping cost: %.2f KZT\n", newShippingCost);
        System.out.printf("New total price (product + shipping): %.2f KZT\n", product.getPrice() + newShippingCost);
        System.out.println("\nSUCCESS: We extended the system's behavior without modifying PhysicalProduct.java.");
    }

    /**
     * DEMO 1: Demonstrates the ProgressiveVat policy.
     */
    private static void demonstrateProgressiveVat(TaxPolicy progressiveVat) {
        System.out.println("=== DEMO 1: New 'Progressive VAT' Tax Policy ===");
        System.out.println("Tax Rules: <=100 -> 5%, 100-500 -> 10%, >500 -> 15%\n");

        // Create dummy products for testing price points
        Product p1 = Product.of("T1", "Low-cost item", 80);
        Product p2 = Product.of("T2", "Mid-cost item", 300);
        Product p3 = Product.of("T3", "High-cost item", 1000);

        System.out.printf("Tax for '%s' (Price: %.2f): %.2f KZT (5%%)\n",
            p1.getName(), p1.getPrice(), progressiveVat.calculateTax(p1, p1.getPrice()));

        System.out.printf("Tax for '%s' (Price: %.2f): %.2f KZT (10%%)\n",
            p2.getName(), p2.getPrice(), progressiveVat.calculateTax(p2, p2.getPrice()));
            
        System.out.printf("Tax for '%s' (Price: %.2f): %.2f KZT (15%%)\n",
            p3.getName(), p3.getPrice(), progressiveVat.calculateTax(p3, p3.getPrice()));
    }

    /**
     * DEMO 2: Demonstrates the plug-and-play nature of the new ShippingPolicy.
     */
    private static void demonstrateShippingPolicies(PhysicalProduct product, ShippingPolicy newPolicy1, ShippingPolicy newPolicy2) {
        System.out.println("\n=== DEMO 2: OCP-compliant Shipping Policies ===");
        System.out.println("Product: " + product.getName() + ' ' + product.getPrice());
        System.out.println("We can change shipping logic at runtime without touching the Product class.\n");

        // 1. Get cost with its default policy
        System.out.printf("1. Initial Shipping Cost (%s): %.2f KZT\n",
            product.getShippingPolicy().getName(), product.getShippingCost());

        // 2. Change policy at runtime
        product.setShippingPolicy(newPolicy1);
        System.out.printf("2. Changed to '%s': %.2f KZT\n",
            product.getShippingPolicy().getName(), product.getShippingCost());
        
        // 3. Change policy again
        product.setShippingPolicy(newPolicy2);
        System.out.printf("3. Changed to '%s': %.2f KZT\n",
            product.getShippingPolicy().getName(), product.getShippingCost());
    }

    /**
     * A generic method to demonstrate full price calculation.
     */
    private static void demonstrateFullPrice(Product product, int qty, List<PricePolicy> policies, TaxPolicy tax) {
        System.out.println("Product: " + product.getName() + ", Quantity: " + qty);
        
        // 1. Apply best promotion from the list
        double subtotal = product.finalPrice(qty, policies);
        System.out.printf("\n1. Subtotal after best promotion: %.2f KZT\n", subtotal);

        // 2. Extract shipping cost if applicable
        double shippingCost = 0;
        if (product instanceof Shippable) {
            shippingCost = ((Shippable) product).getShippingCost();
        }
        double taxableAmount = subtotal; // Tax is not applied on shipping
        System.out.printf("2. Taxable amount (subtotal): %.2f KZT\n", taxableAmount);

        // 3. Calculate tax
        double taxAmount = tax.calculateTax(product, taxableAmount);
        System.out.printf("3. Tax (%s): %.2f KZT\n", tax.name(), taxAmount);

        // 4. Shipping cost
        System.out.printf("4. Shipping Cost: %.2f KZT\n", shippingCost);

        // 5. Final price
        double finalTotal = taxableAmount + taxAmount + shippingCost;
        System.out.printf("\n5. FINAL TOTAL: %.2f KZT\n", finalTotal);
        System.out.println("   (Taxable: " + String.format("%.2f", taxableAmount) +
                         " + Tax: " + String.format("%.2f", taxAmount) +
                         " + Shipping: " + String.format("%.2f", shippingCost) + ")");
    }
}

