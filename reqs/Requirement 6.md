Practice 5 — Polymorphism: Price Policies + Method Overloading/Overriding
Builds on: Task 1–3 (Product with encapsulation/constructors/static), Task 4 (PhysicalProduct, DigitalProduct).
Scope: Keep the project minimal: Products only (no Orders/Payments).

🎯 Goal
Demonstrate polymorphism with a unified price rules interface, plus overloading (compile-time polymorphism) and overriding (runtime polymorphism) on Product subclasses.
You will:
1.	Introduce PricePolicy (an interface for pricing rules).
2.	Implement three rules: PercentageOff, FixedOff, BogoHalf (buy-one-get-one-half).
3.	Overload finalPrice(...) methods in Product.
4.	Override price calculation in PhysicalProduct and DigitalProduct.
5.	Use a polymorphic list (List<Product>, List<PricePolicy>) in a demo to show different behavior per subtype.

📁 Project Structure
src/
└── product/
    ├── Product.java
    ├── PhysicalProduct.java
    ├── DigitalProduct.java
    ├── pricing/
    │   ├── PricePolicy.java
    │   ├── PercentageOff.java
    │   ├── FixedOff.java
    │   └── BogoHalf.java
    └── ShopDemo5.java

1) Interface: PricePolicy
// product/pricing/PricePolicy.java
package product.pricing;

import product.Product;

public interface PricePolicy {
    /** Human-readable rule name (for printing). */
    String name();

    /**
     * Calculate the FINAL cost for 'qty' units of 'p' (NO shipping here).
     * Must NOT mutate Product.
     */
    double apply(Product p, int qty);

    /** By default, applicable to all products. */
    default boolean applicableTo(Product p) { return true; }
}
Implementations
// product/pricing/PercentageOff.java
package product.pricing;
import product.Product;

public class PercentageOff implements PricePolicy {
    private final double percent; // 0..90

    public PercentageOff(double percent) {
        this.percent = Math.max(0, Math.min(90, percent));
    }

    @Override public String name() { return "Percent-" + percent + "%"; }

    @Override public double apply(Product p, int qty) {
        double unit = p.getPrice() * (1 - percent / 100.0);
        return unit * Math.max(0, qty);
    }
}
// product/pricing/FixedOff.java
package product.pricing;
import product.Product;

public class FixedOff implements PricePolicy {
    private final double amount; // >= 0

    public FixedOff(double amount) { this.amount = Math.max(0, amount); }

    @Override public String name() { return "Fixed-" + amount; }

    @Override public double apply(Product p, int qty) {
        double unit = Math.max(0.0, p.getPrice() - amount);
        return unit * Math.max(0, qty);
    }
}
// product/pricing/BogoHalf.java
// For every pair: second item is -50%.
// Price for a pair = 1.5 * price; average = 0.75 * price per unit.
package product.pricing;
import product.Product;

public class BogoHalf implements PricePolicy {
    @Override public String name() { return "BOGO-HALF"; }

    @Override public double apply(Product p, int qty) {
        double price = p.getPrice();
        int pairs = Math.max(0, qty) / 2;
        int singles = Math.max(0, qty) % 2;
        return pairs * (price * 1.5) + singles * price;
    }
}

2) Product: Method Overloading
Add overloads to Product (keep your existing fields/guards from previous tasks):
// Product.java — add these overloads

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

3) Subclasses: Method Overriding
PhysicalProduct
Add shipping once per order (after discounts). Reuse your estimateShippingCost() from Task 4.
// PhysicalProduct.java — override single-rule version
@Override
public double finalPrice(int qty, product.pricing.PricePolicy policy) {
    double base = super.finalPrice(qty, policy); // rule applied
    return base + estimateShippingCost();        // add shipping once
}

// (optional) also override the List<PricePolicy> version to add shipping to the chosen best:
@Override
public double finalPrice(int qty, java.util.List<product.pricing.PricePolicy> policies) {
    double base = super.finalPrice(qty, policies);
    return base + estimateShippingCost();
}
DigitalProduct
Ignore BOGO-half (digital items don’t participate). Two options:
•	(Simple) Handle in DigitalProduct.finalPrice(...).
•	(Clean) Override applicableTo in BogoHalf to return false for DigitalProduct (optional).
Simple option:
// DigitalProduct.java
@Override
public double finalPrice(int qty, product.pricing.PricePolicy policy) {
    if (policy instanceof product.pricing.BogoHalf) {
        return super.finalPrice(qty); // ignore BOGO-half for digital
    }
    return super.finalPrice(qty, policy);
}

4) Demo — ShopDemo5 (Polymorphic Lists)
// product/ShopDemo5.java
package product;

import product.pricing.*;
import java.util.List;

public class ShopDemo5 {
    public static void main(String[] args) {
        PhysicalProduct laptop = new PhysicalProduct("P-LAP-1","Laptop",450_000.0,1.8);
        laptop.trySetDimensions(35,24,2);

        DigitalProduct ebook = new DigitalProduct("P-EBK-1","E-Book",1_500.0,12.5);

        List<Product> items = List.of(laptop, ebook);
        List<PricePolicy> rules = List.of(new PercentageOff(10), new FixedOff(50), new BogoHalf());

        for (Product p : items) {
            for (int qty : new int[]{1, 2}) {
                System.out.println("\n== " + p.getName() + " | qty=" + qty);
                System.out.println("Base: " + p.finalPrice(qty));

                for (PricePolicy r : rules) {
                    System.out.println(r.name() + ": " + p.finalPrice(qty, r));
                }
                System.out.println("Best(of all): " + p.finalPrice(qty, rules));
            }
        }
    }
}
Expected observations
•	PhysicalProduct adds shipping on top of any rule.
•	DigitalProduct ignores BogoHalf but accepts other rules.
•	Overloading chooses method by signature; overriding resolves by runtime type.

