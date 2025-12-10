package product;

import category.Category;

public class ShopDemo3 {
    public static void main(String[] args) {
        System.out.println("=== Requirement 4 Demo: Constructors & Static Factory ===\n");

        Category electronics = new Category(1, "Electronics", "Electronic devices");

        System.out.println("--- 1. No-args Constructor (Auto-generated defaults) ---\n");

        Product p1 = new Product();
        System.out.println("Product created with no arguments:");
        System.out.println("   ID: " + p1.getId());
        System.out.println("   Name: " + p1.getName());
        System.out.println("   Price: " + p1.getPrice() + " " + Product.DEFAULT_CURRENCY);
        System.out.println("   Quantity: " + p1.getQuantity());
        System.out.println();

        System.out.println("--- 2. Required-args Constructor (id, name, price) ---\n");

        Product p2 = new Product("P100", "Notebook", 950.0);
        System.out.println("Product created with (id, name, price):");
        System.out.println("   ID: " + p2.getId());
        System.out.println("   Name: " + p2.getName());
        p2.applyDiscount(20);
        System.out.println("   Price: " + p2.getPrice() + " " + Product.DEFAULT_CURRENCY);
        System.out.println("   Quantity: " + p2.getQuantity());
        System.out.println();

        System.out.println("--- 3. Full-args Constructor (all parameters) ---\n");

        Product p3 = new Product("H100", "Headphones", "BT 5.0", 14990.0, 5, electronics);
        System.out.println("Product created with all parameters:");
        System.out.println("   ID: " + p3.getId());
        System.out.println("   Name: " + p3.getName());
        System.out.println("   Description: " + p3.getDescription());
        System.out.println("   Price: " + p3.getPrice() + " " + Product.DEFAULT_CURRENCY);
        System.out.println("   Quantity: " + p3.getQuantity());
        System.out.println("   Category: " + (p3.getCategory() != null ? p3.getCategory().getName() : "None"));
        System.out.println();

        System.out.println("--- 4. Static Factory: Product.of() ---\n");

        Product p4 = Product.of("S100", "Pencil", 120.0);
        System.out.println("Product created with Product.of(id, name, price):");
        System.out.println("   ID: " + p4.getId());
        System.out.println("   Name: " + p4.getName());
        System.out.println("   Price: " + p4.getPrice() + " " + Product.DEFAULT_CURRENCY);
        System.out.println();

        System.out.println("--- 5. Static Factory: Product.freeSample() ---\n");

        Product p5 = Product.freeSample("Sticker");
        System.out.println("Product created with Product.freeSample(name):");
        System.out.println("   ID: " + p5.getId() + " (auto-generated)");
        System.out.println("   Name: " + p5.getName());
        System.out.println("   Price: " + p5.getPrice() + " " + Product.DEFAULT_CURRENCY + " (free)");
        System.out.println("   Quantity: " + p5.getQuantity());
        System.out.println();

        System.out.println("--- Auto-ID Sequence Demo ---\n");

        Product auto1 = new Product();
        Product auto2 = new Product();
        Product auto3 = new Product();

        System.out.println("3 products created with no-args constructor:");
        System.out.println("   Product 1 ID: " + auto1.getId());
        System.out.println("   Product 2 ID: " + auto2.getId());
        System.out.println("   Product 3 ID: " + auto3.getId());
        System.out.println();

        System.out.println("--- Guards Test: Invalid Parameters ---\n");

        Product invalid = new Product("", "A", -5);
        System.out.println("Attempting to create with invalid data (\"\", \"A\", -5):");
        System.out.println("   ID: " + invalid.getId() + " (validation failed, kept auto-ID)");
        System.out.println("   Name: " + invalid.getName() + " (validation failed, kept default)");
        System.out.println("   Price: " + invalid.getPrice() + " (validation failed, kept default)");
        System.out.println();

        System.out.println("--- Static Members Info ---\n");

        System.out.println("DEFAULT_CURRENCY: " + Product.DEFAULT_CURRENCY);
        System.out.println("Total products created: " + Product.getCreatedCount());
        System.out.println();

        System.out.println("=== Demo Complete ===");
    }
}
