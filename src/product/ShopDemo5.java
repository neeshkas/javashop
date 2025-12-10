package product;

import product.pricing.*;
import product.shipping.ShippingPolicy;
import product.shipping.SimpleWeightShippingPolicy;

import java.util.List;

/**
 * ShopDemo5 - Demonstration of Polymorphism
 *
 * This demo showcases:
 * 1. Interface-based polymorphism (PricePolicy interface with multiple implementations)
 * 2. Method overloading (compile-time polymorphism) - multiple finalPrice() signatures
 * 3. Method overriding (runtime polymorphism) - PhysicalProduct adds shipping,
 *    DigitalProduct ignores BogoHalf
 * 4. Polymorphic collections (List<Product>, List<PricePolicy>)
 */
public class ShopDemo5 {
    public static void main(String[] args) {
        // A default policy is now needed to instantiate PhysicalProducts
        ShippingPolicy defaultShipping = new SimpleWeightShippingPolicy(1000); // 1000 KZT per Kg

        // Create products
        PhysicalProduct laptop = new PhysicalProduct("P-LAP-1", "Laptop", 450_000.0, 1.8, defaultShipping);
        laptop.trySetDimensions(35, 24, 2);

        DigitalProduct ebook = new DigitalProduct("P-EBK-1", "E-Book", 1_500.0, 12.5);

        // Polymorphic list of products
        List<Product> items = List.of(laptop, ebook);

        // Polymorphic list of pricing policies
        List<PricePolicy> rules = List.of(
            new PercentageOff(10),
            new FixedOff(50),
            new BogoHalf()
        );

        System.out.println("=".repeat(70));
        System.out.println("   SHOP DEMO 5: POLYMORPHISM IN ACTION");
        System.out.println("=".repeat(70));

        // Iterate through products and quantities
        for (Product p : items) {
            for (int qty : new int[]{2, 4}) {
                System.out.println("\n== " + p.getName() + " | qty=" + qty);
                System.out.println("   Base price: " + String.format("%.2f", p.finalPrice(qty)) + " KZT");

                for (PricePolicy r : rules) {
                    double finalPrice = p.finalPrice(qty, r);
                    System.out.println("   " + String.format("%-15s", r.name() + ":") +
                                     String.format("%.2f", finalPrice) + " KZT");
                }

                double bestPrice = p.finalPrice(qty, rules);
                System.out.println("   " + String.format("%-15s", "Best(of all):") +
                                 String.format("%.2f", bestPrice) + " KZT");
            }
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("   KEY OBSERVATIONS:");
        System.out.println("=".repeat(70));
        System.out.println("1. PhysicalProduct adds shipping cost on top of any pricing rule");
        System.out.println("2. DigitalProduct ignores BOGO-HALF but accepts other rules");
        System.out.println("3. Method overloading: finalPrice() has 4 different signatures");
        System.out.println("4. Method overriding: Each subclass implements specific behavior");
        System.out.println("5. Polymorphism: Same interface, different implementations");
        System.out.println("=".repeat(70));
    }
}
