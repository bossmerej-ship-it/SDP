package assignment4.service;

import assignment4.model.Customer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Creates and cancels simulated shipments. The demo courier delivers within Kazakhstan. */
public class ShippingService {
    private final Map<UUID, String> shipments = new HashMap<>();

    public String schedule(UUID orderId, Customer customer) {
        Objects.requireNonNull(orderId, "Order ID is required.");
        Objects.requireNonNull(customer, "Customer is required.");
        if (!"KZ".equals(customer.countryCode())) {
            throw new IllegalStateException("Delivery is available only within Kazakhstan (KZ).");
        }
        if (shipments.containsKey(orderId)) {
            throw new IllegalStateException("Delivery is already scheduled for this order.");
        }
        String trackingNumber = "KZ-" + orderId.toString().toUpperCase(java.util.Locale.ROOT);
        shipments.put(orderId, trackingNumber);
        return trackingNumber;
    }

    public void cancel(UUID orderId) {
        shipments.remove(orderId);
    }

    public boolean isScheduled(UUID orderId) {
        return shipments.containsKey(orderId);
    }

    public int activeShipmentCount() {
        return shipments.size();
    }
}
