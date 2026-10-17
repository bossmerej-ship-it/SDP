package assignment4.model;

import java.math.BigDecimal;

/** A catalog snapshot. Prices are in KZT. */
public record Product(String sku, String name, BigDecimal unitPrice, int availableQuantity) {
    public Product {
        if (sku == null || sku.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product SKU and name are required.");
        }
        if (unitPrice == null || unitPrice.signum() <= 0 || availableQuantity < 0) {
            throw new IllegalArgumentException("Price must be positive and stock cannot be negative.");
        }
    }
}
