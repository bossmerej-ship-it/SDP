package assignment4.model;

import java.util.Map;

/** An immutable shopping cart; quantities must be positive. */
public record Cart(Map<String, Integer> items) {
    public Cart {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A cart must contain at least one product.");
        }
        for (var entry : items.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()
                    || entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Each cart item needs a SKU and a positive quantity.");
            }
        }
        items = Map.copyOf(items);
    }
}
