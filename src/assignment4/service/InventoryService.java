package assignment4.service;

import assignment4.model.Cart;
import assignment4.model.Product;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/** Owns the catalog, stock, pricing, and reservations. */
public class InventoryService {
    private final Map<String, Product> catalog = new TreeMap<>();
    private final Map<String, Integer> stock = new HashMap<>();
    private final Map<UUID, Cart> reservations = new HashMap<>();

    public InventoryService(List<Product> products) {
        for (Product product : products) {
            if (catalog.putIfAbsent(product.sku(), product) != null) {
                throw new IllegalArgumentException("Duplicate SKU: " + product.sku());
            }
            stock.put(product.sku(), product.availableQuantity());
        }
    }

    public List<Product> listProducts() {
        return catalog.values().stream().map(product -> new Product(product.sku(), product.name(),
                product.unitPrice(), stock.get(product.sku()))).toList();
    }

    /** Prices every item before any stock is changed. */
    public BigDecimal calculateTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (var item : cart.items().entrySet()) {
            Product product = requireProduct(item.getKey());
            total = total.add(product.unitPrice().multiply(BigDecimal.valueOf(item.getValue())));
        }
        return total;
    }

    /** Validates the complete cart first, so a failed multi-item reservation changes nothing. */
    public void reserve(UUID orderId, Cart cart) {
        Objects.requireNonNull(orderId, "Order ID is required.");
        Objects.requireNonNull(cart, "Cart is required.");
        if (reservations.containsKey(orderId)) {
            throw new IllegalStateException("Stock is already reserved for this order.");
        }
        for (var item : cart.items().entrySet()) {
            requireProduct(item.getKey());
            if (stock.get(item.getKey()) < item.getValue()) {
                throw new IllegalStateException("Insufficient stock for " + item.getKey());
            }
        }
        cart.items().forEach((sku, quantity) -> stock.put(sku, stock.get(sku) - quantity));
        reservations.put(orderId, cart);
    }

    /** Idempotent compensation: releasing the same order twice never adds extra stock. */
    public void release(UUID orderId) {
        Cart cart = reservations.remove(orderId);
        if (cart != null) {
            cart.items().forEach((sku, quantity) -> stock.put(sku, stock.get(sku) + quantity));
        }
    }

    public boolean hasReservation(UUID orderId) {
        return reservations.containsKey(orderId);
    }

    private Product requireProduct(String sku) {
        Product product = catalog.get(sku);
        if (product == null) {
            throw new IllegalArgumentException("Unknown product: " + sku);
        }
        return product;
    }
}
