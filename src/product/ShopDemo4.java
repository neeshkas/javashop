package product;

import category.Category;
import product.shipping.ShippingPolicy;
import product.shipping.SimpleWeightShippingPolicy;

public class ShopDemo4 {
    public static void main(String[] args) {
        System.out.println("=== Requirement 5 Demo: Product Subtypes (Physical & Digital) ===\n");

        // A default policy is now needed to instantiate PhysicalProducts
        ShippingPolicy defaultShipping = new SimpleWeightShippingPolicy(1000); // 1000 KZT per Kg

        Category electronics = new Category(1, "Electronics", "Electronic devices and gadgets");

        System.out.println("--- 1. PhysicalProduct: No-args Constructor ---\n");

        PhysicalProduct laptop1 = new PhysicalProduct();
        laptop1.setShippingPolicy(defaultShipping); // Policy must be set
        System.out.println("Created with no-args constructor:");
        System.out.println(laptop1);
        System.out.println();

        System.out.println("--- 2. PhysicalProduct: Required-args Constructor ---\n");

        PhysicalProduct laptop2 = new PhysicalProduct("LAP100", "Gaming Laptop", 250000.0, 2.5, defaultShipping);
        System.out.println("Created with (id, name, price, weight, policy):");
        System.out.println(laptop2);
        System.out.println("Shipping cost: " + laptop2.getShippingCost() + " " + Product.DEFAULT_CURRENCY);
        System.out.println();

        System.out.println("--- 3. PhysicalProduct: Full-args Constructor ---\n");

        PhysicalProduct laptop3 = new PhysicalProduct("LAP200", "Business Laptop",
                                                      "15.6 inch display, 16GB RAM",
                                                      180000.0, 10, 2.3, 35.0, 25.0, 2.5, defaultShipping);
        System.out.println("Created with all parameters:");
        System.out.println(laptop3);
        System.out.println("Shipping cost: " + laptop3.getShippingCost() + " " + Product.DEFAULT_CURRENCY);
        System.out.println("Final price: " + laptop3.finalPrice(laptop3.getQuantity()));
        System.out.println();

        System.out.println("--- 4. PhysicalProduct: Valid Updates ---\n");

        boolean result1 = laptop3.trySetDimensions(40.0, 30.0, 3.0);
        System.out.println("trySetDimensions(40, 30, 3): " + result1);
        System.out.println("New shipping cost: " + laptop3.getShippingCost() + " " + Product.DEFAULT_CURRENCY);

        boolean result2 = laptop3.trySetWeightKg(3.0);
        System.out.println("trySetWeightKg(3.0): " + result2);
        System.out.println("New shipping cost: " + laptop3.getShippingCost() + " " + Product.DEFAULT_CURRENCY);
        System.out.println();

        System.out.println("--- 5. PhysicalProduct: Invalid Updates ---\n");

        boolean result3 = laptop3.trySetDimensions(-1, 10, 10);
        System.out.println("trySetDimensions(-1, 10, 10): " + result3 + " (negative dimension rejected)");

        boolean result4 = laptop3.trySetWeightKg(5000);
        System.out.println("trySetWeightKg(5000): " + result4 + " (exceeds max 1000kg)");
        System.out.println();

        System.out.println("--- 6. DigitalProduct: No-args Constructor ---\n");

        DigitalProduct ebook1 = new DigitalProduct();
        System.out.println("Created with no-args constructor:");
        System.out.println(ebook1);
        System.out.println();

        System.out.println("--- 7. DigitalProduct: Required-args Constructor ---\n");

        DigitalProduct ebook2 = new DigitalProduct("EBOOK100", "Java Programming Guide", 5000.0, 15.5);
        System.out.println("Created with (id, name, price, downloadSize):");
        System.out.println(ebook2);
        System.out.println("License required: " + ebook2.isLicenseRequired());
        System.out.println();

        System.out.println("--- 8. DigitalProduct: Full-args Constructor ---\n");

        DigitalProduct software = new DigitalProduct("SOFT200", "Photo Editor Pro",
                                                     "Professional photo editing software",
                                                     45000.0, 50, 850.0, "PRO-2024-XYZ123");
        System.out.println("Created with all parameters:");
        System.out.println(software);
        System.out.println("License required: " + software.isLicenseRequired());
        System.out.println();

        System.out.println("--- 9. DigitalProduct: Valid Updates ---\n");

        boolean result5 = software.trySetDownloadSizeMb(2048);
        System.out.println("trySetDownloadSizeMb(2048): " + result5);
        System.out.println("New download size: " + software.getDownloadSizeMb() + " MB");

        boolean result6 = software.trySetLicenseKey("ULTIMATE-2024-ABC999");
        System.out.println("trySetLicenseKey('ULTIMATE-2024-ABC999'): " + result6);
        System.out.println("License required: " + software.isLicenseRequired());
        System.out.println();

        System.out.println("--- 10. DigitalProduct: Invalid Updates ---\n");

        boolean result7 = software.trySetDownloadSizeMb(-50);
        System.out.println("trySetDownloadSizeMb(-50): " + result7 + " (negative size rejected)");

        String longKey = "A".repeat(100);
        boolean result8 = software.trySetLicenseKey(longKey);
        System.out.println("trySetLicenseKey(100 chars): " + result8 + " (exceeds max 64 chars)");
        System.out.println();

        System.out.println("--- 11. Testing License Requirement Logic ---\n");

        DigitalProduct noLicense = new DigitalProduct("FREE100", "Free eBook", 0.0, 5.0);
        System.out.println("Product without license:");
        System.out.println("License required: " + noLicense.isLicenseRequired());

        noLicense.trySetLicenseKey("NEW-LICENSE-KEY");
        System.out.println("After setting license key:");
        System.out.println("License required: " + noLicense.isLicenseRequired());

        noLicense.trySetLicenseKey(null);
        System.out.println("After removing license key:");
        System.out.println("License required: " + noLicense.isLicenseRequired());

        noLicense.trySetLicenseKey("   ");
        System.out.println("After setting blank license key:");
        System.out.println("License required: " + noLicense.isLicenseRequired());
        System.out.println();

        System.out.println("--- 12. Polymorphism Demo ---\n");

        Product[] products = {laptop3, software, ebook2};

        System.out.println("All products in inventory:");
        for (Product p : products) {
            System.out.println(p);
        }
        System.out.println();

        System.out.println("--- 13. Inheritance Verification ---\n");

        System.out.println("laptop3 instanceof Product: " + (laptop3 instanceof Product));
        System.out.println("laptop3 instanceof PhysicalProduct: " + (laptop3 instanceof PhysicalProduct));
        System.out.println("software instanceof Product: " + (software instanceof Product));
        System.out.println("software instanceof DigitalProduct: " + (software instanceof DigitalProduct));
        System.out.println();

        System.out.println("--- 14. Shipping Cost Calculations ---\n");

        PhysicalProduct lightBox = new PhysicalProduct("BOX1", "Small Box", 100.0, 0.5, defaultShipping);
        lightBox.trySetDimensions(10.0, 10.0, 10.0);
        System.out.println("Light box (0.5kg, 10x10x10cm):");
        System.out.println("Shipping cost: " + lightBox.getShippingCost() + " " + Product.DEFAULT_CURRENCY);

        PhysicalProduct heavyBox = new PhysicalProduct("BOX2", "Heavy Box", 100.0, 15.0, defaultShipping);
        heavyBox.trySetDimensions(20.0, 20.0, 20.0);
        System.out.println("\nHeavy box (15kg, 20x20x20cm):");
        System.out.println("Shipping cost: " + heavyBox.getShippingCost() + " " + Product.DEFAULT_CURRENCY);

        PhysicalProduct largeBox = new PhysicalProduct("BOX3", "Large Box", 100.0, 5.0, defaultShipping);
        largeBox.trySetDimensions(100.0, 100.0, 100.0);
        System.out.println("\nLarge box (5kg, 100x100x100cm):");
        System.out.println("Shipping cost: " + largeBox.getShippingCost() + " " + Product.DEFAULT_CURRENCY);
        System.out.println();

        System.out.println("--- Summary ---\n");
        System.out.println("Total products created: " + Product.getCreatedCount());
        System.out.println("Default currency: " + Product.DEFAULT_CURRENCY);

        System.out.println("\n=== Demo Complete ===");
    }
}
