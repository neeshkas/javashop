package product;

import category.Category;
import java.util.ArrayList;
import java.util.List;

public class Product {
    public static final String DEFAULT_CURRENCY = "KZT";
    private static int SEQ = 1;
    private static int createdCount = 0;
    private static List<Product> allProducts = new ArrayList<>();
 
    private String id;
    private String name;
    private String description;
    private double price;
    private int quantity;
    private Category category;

    public Product() {
        this.id = nextSeq();
        this.name = "Unnamed";
        this.description = null;
        this.price = 0.0;
        this.quantity = 0;
        this.category = null;

        createdCount++;
        allProducts.add(this);
    }

    public Product(String id, String name, double price) {
        this();

        trySetId(id);
        trySetName(name);
        trySetPrice(price);
    }

    public Product(String id, String name, String description, double price, int quantity, Category category) {
        this();

        trySetId(id);
        trySetName(name);
        trySetDescription(description);
        trySetPrice(price);
        trySetQuantity(quantity);
        trySetCategory(category);
    }

    private static String nextSeq() {
        return String.valueOf(SEQ++);
    }

    public static int getCreatedCount() {
        return createdCount;
    }

    public static Product of(String id, String name, double price) {
        return new Product(id, name, price);
    }

    public static Product freeSample(String name) {
        Product product = new Product();
        product.trySetName(name);
        product.trySetPrice(0.0);
        product.trySetQuantity(1);
        return product;
    }

    public boolean trySetId(String id) {
        if (id == null || id.trim().length() < 2) {
            return false;
        }

        String uniqueId = id.trim();

        // Проверка на уникальность
        for (Product p : allProducts) {
            if (p.getId() != null && p.getId().equals(uniqueId)) {
                return false;
            }
        }

        this.id = uniqueId;
        return true;
    }

    public boolean trySetName(String name) {
        if (name != null && name.trim().length() >= 2) {
            this.name = name.trim();
            return true;
        }
        return false;
    }

    public boolean trySetDescription(String description) {
        if (description == null) {
            this.description = null;
            return true;
        }
        String trimmed = description.trim();
        if (trimmed.length() <= 200) {
            this.description = trimmed;
            return true;
        }
        return false;
    }

    public boolean trySetPrice(double price) {
        if (price >= 0.0 && price <= 1_000_000.0) {
            this.price = price;
            return true;
        }
        return false;
    }

    public boolean trySetQuantity(int quantity) {
        if (quantity >= 0 && quantity <= 1_000_000) {
            this.quantity = quantity;
            return true;
        }
        return false;
    }

    public boolean trySetCategory(Category category) {
        this.category = category;
        return category != null;
    }

    public boolean addStock(int amount) {
        if (amount > 0 && (this.quantity + amount) <= 1_000_000) {
            this.quantity += amount;
            return true;
        }
        return false;
    }

    public boolean sellProduct(int amount) {
        if (amount > 0 && amount <= this.quantity) {
            this.quantity -= amount;
            return true;
        }
        return false;
    }

    public boolean applyDiscount(double percent) {
        if (percent >= 0 && percent <= 90) {
            this.price -= this.price * (percent / 100);
            return true;
        }
        return false;
    }

    public double calculateTotalValue() {
        return this.price * this.quantity;
    }

    /**
     * Method Overloading: finalPrice() with different signatures
     * Demonstrates compile-time polymorphism
     */

    // 1) Single unit, no rules
    public double finalPrice() {
        return getPrice();
    }

    // 2) qty units, no rules
    public double finalPrice(int qty) {
        if (qty <= 0) return 0.0;
        return getPrice() * qty;
    }

    // 3) qty + single rule (no shipping here; subclasses may add it)
    public double finalPrice(int qty, product.pricing.PricePolicy policy) {
        if (qty <= 0) return 0.0;
        if (policy == null || !policy.applicableTo(this)) return finalPrice(qty);
        return policy.apply(this, qty);
    }

    // 4) qty + list of rules (choose the best = minimal price)
    public double finalPrice(int qty, java.util.List<product.pricing.PricePolicy> policies) {
        if (qty <= 0) return 0.0;
        if (policies == null || policies.isEmpty()) return finalPrice(qty);
        double best = Double.POSITIVE_INFINITY;
        for (var pp : policies) {
            double v = finalPrice(qty, pp);
            if (v < best) best = v;
        }
        return best;
    }

    //3 по цене 2 - для корзины товаров
    //public static double calculateCartWithDiscount(List<Product> cart) {
      //  if (cart == null || cart.size() < 3) {
        //    if (cart == null) {
          //      return 0.0;
            //} else {
              //  return cart.stream().mapToDouble(Product::getPrice).sum();
            //}

        //}
        //double total = cart.stream().mapToDouble(Product::getPrice).sum();
        //double cheapest = cart.stream().mapToDouble(Product::getPrice).min().orElse(0.0);
        //return total - cheapest;
    //a}

    public String getStockStatus() {
        if (quantity == 0) {
            return "OUT_OF_STOCK";
        } else if (quantity >= 1 && quantity <= 10) {
            return "LOW";
        } else {
            return "IN_STOCK";
        }
    }

    public void displayProductInfo() {
        System.out.println("Product Info:");
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Price: $" + price);
        System.out.println("Quantity: " + quantity);
        System.out.println("Stock Status: " + getStockStatus());
        System.out.println("Category: " + (category != null ? category.getName() : "No category"));
        System.out.println("Total Stock Value: $" + calculateTotalValue());
        System.out.println();
    }

    public String getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public double getPrice() { return price; }

    public int getQuantity() { return quantity; }

    public Category getCategory() { return category; }

    @Override
    public String toString() {
        return "Product{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", price=" + price + " " + DEFAULT_CURRENCY +
               ", quantity=" + quantity +
               ", stockStatus='" + getStockStatus() + '\'' +
               ", category=" + (category != null ? category.getName() : "None") +
               '}';
    }
}
