package category;

import product.Product;
import java.util.ArrayList;
import java.util.List;

public class Category {
    private int id;
    private String name;
    private String description;
    private List<Product> products;

    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.products = new ArrayList<>();
    }

    public boolean addProduct(Product product) {
        if (product == null) {
            return false;
        }
        if (products.contains(product)) {
            return false;
        }
        products.add(product);
        product.trySetCategory(this);
        return true;
    }

    public void removeProduct(Product product) {
        if (products.remove(product)) {
            product.trySetCategory(null);
        }
    }

    public double getTotalValue() {
        double total = 0;
        for (Product p : products) {
            total += p.calculateTotalValue();
        }
        return total;
    }

    public void displayCategoryInfo() {
        System.out.println("Category Info:");
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
        System.out.println("Total Category Value: $" + getTotalValue());
        System.out.println("Products in this category:");
        for (Product p : products) {
            System.out.println("- " + p.getName());
        }
        System.out.println();
    }

    public String getName() {
        return name;
    }
}
