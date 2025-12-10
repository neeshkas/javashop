package product;

import category.Category;

public class DigitalProduct extends Product {
    private double downloadSizeMb;
    private String licenseKey;

    public DigitalProduct() {
        super();
        this.downloadSizeMb = 0.0;
        this.licenseKey = null;
    }

    public DigitalProduct(String id, String name, double price, double downloadSizeMb) {
        super(id, name, price);
        trySetDownloadSizeMb(downloadSizeMb);
        this.licenseKey = null;
    }

    public DigitalProduct(String id, String name, String description, double price, int quantity, double downloadSizeMb, String licenseKey) {
        super(id, name, description, price, quantity, null);
        trySetDownloadSizeMb(downloadSizeMb);
        trySetLicenseKey(licenseKey);
    }

    public boolean trySetDownloadSizeMb(double downloadSizeMb) {
        if (downloadSizeMb >= 0.0 && downloadSizeMb <= 1_000_000.0) {
            this.downloadSizeMb = downloadSizeMb;
            return true;
        }
        return false;
    }

    public boolean trySetLicenseKey(String licenseKey) {
        if (licenseKey == null) {
            this.licenseKey = null;
            return true;
        }
        if (licenseKey.length() <= 64) {
            this.licenseKey = licenseKey;
            return true;
        }
        return false;
    }

    public boolean isLicenseRequired() {
        return licenseKey != null && !licenseKey.isBlank();
    }

    public double getDownloadSizeMb() {
        return downloadSizeMb;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    /**
     * Method Overriding: Digital products ignore BOGO-HALF rule
     * Demonstrates runtime polymorphism with type-specific behavior
     */

    @Override
    public double finalPrice(int qty, product.pricing.PricePolicy policy) {
        if (policy instanceof product.pricing.BogoHalfPromotion) {
            return super.finalPrice(qty); // ignore BOGO-half for digital
        }
        return super.finalPrice(qty, policy);
    }

    @Override
    public String toString() {
        return "DigitalProduct{" +
               "id='" + getId() + '\'' +
               ", name='" + getName() + '\'' +
               ", price=" + getPrice() + " " + DEFAULT_CURRENCY +
               ", quantity=" + getQuantity() +
               ", downloadSizeMb=" + downloadSizeMb +
               ", licenseRequired=" + isLicenseRequired() +
               (licenseKey != null ? ", licenseKey='" + licenseKey + '\'' : "") +
               '}';
    }
}
