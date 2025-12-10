package product;

import product.shipping.Shippable;
import product.shipping.ShippingPolicy;

public class PhysicalProduct extends Product implements Shippable {
    private double weightKg;
    private double lengthCm;
    private double widthCm;
    private double heightCm;
    private ShippingPolicy shippingPolicy;

    public PhysicalProduct() {
        super();
        this.weightKg = 0.0;
        this.lengthCm = 0.0;
        this.widthCm = 0.0;
        this.heightCm = 0.0;
        this.shippingPolicy = null; // Policy must be set later
    }

    public PhysicalProduct(String id, String name, double price, double weightKg, ShippingPolicy shippingPolicy) {
        super(id, name, price);
        trySetWeightKg(weightKg);
        this.lengthCm = 0.0;
        this.widthCm = 0.0;
        this.heightCm = 0.0;
        this.shippingPolicy = shippingPolicy;
    }

    public PhysicalProduct(String id, String name, String description, double price,
                          int quantity, double weightKg, double lengthCm,
                          double widthCm, double heightCm, ShippingPolicy shippingPolicy) {
        super(id, name, description, price, quantity, null);
        trySetWeightKg(weightKg);
        trySetDimensions(lengthCm, widthCm, heightCm);
        this.shippingPolicy = shippingPolicy;
    }

    public ShippingPolicy getShippingPolicy() {
        return shippingPolicy;
    }

    public void setShippingPolicy(ShippingPolicy shippingPolicy) {
        this.shippingPolicy = shippingPolicy;
    }

    public boolean trySetWeightKg(double weightKg) {
        if (weightKg >= 0.0 && weightKg <= 1000.0) {
            this.weightKg = weightKg;
            return true;
        }
        return false;
    }

    public boolean trySetDimensions(double lengthCm, double widthCm, double heightCm) {
        if (lengthCm >= 0.0 && lengthCm <= 1000.0 &&
            widthCm >= 0.0 && widthCm <= 1000.0 &&
            heightCm >= 0.0 && heightCm <= 1000.0) {
            this.lengthCm = lengthCm;
            this.widthCm = widthCm;
            this.heightCm = heightCm;
            return true;
        }
        return false;
    }

    /**
     * Implements Shippable interface: returns shipping cost by delegating to a shipping policy.
     * @throws IllegalStateException if the shipping policy has not been set.
     */
    @Override
    public double getShippingCost() {
        if (shippingPolicy == null) {
            // Or return a default cost, or handle as an error
            throw new IllegalStateException("Shipping policy not set for product: " + getName());
        }
        return shippingPolicy.calculateCost(this);
    }

    /**
     * Implements Shippable interface: physical products always require shipping.
     */
    @Override
    public boolean requiresShipping() {
        return true;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public double getLengthCm() {
        return lengthCm;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    @Override
    public String toString() {
        return "PhysicalProduct{" +
               "id='" + getId() + '\'' +
               ", name='" + getName() + '\'' +
               ", price=" + getPrice() + " " + DEFAULT_CURRENCY +
               ", quantity=" + getQuantity() +
               ", weightKg=" + weightKg +
               ", dimensions=" + lengthCm + "x" + widthCm + "x" + heightCm + " cm" +
               ", shippingPolicy=" + (shippingPolicy != null ? shippingPolicy.getName() : "None") +
               ", stockStatus='" + getStockStatus() + '\'' +
               '}';
    }
}
