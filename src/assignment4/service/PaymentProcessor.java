package assignment4.service;

import assignment4.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** An in-memory gateway that records charges and refunds using exact decimal amounts. */
public class PaymentProcessor {
    private final Map<UUID, BigDecimal> charges = new HashMap<>();

    public void charge(UUID orderId, BigDecimal amount, PaymentMethod method) {
        Objects.requireNonNull(orderId, "Order ID is required.");
        Objects.requireNonNull(method, "Payment method is required.");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        if (charges.containsKey(orderId)) {
            throw new IllegalStateException("This order has already been charged.");
        }
        if (method == PaymentMethod.DECLINED_CARD) {
            throw new IllegalStateException("Payment declined by the gateway.");
        }
        charges.put(orderId, amount);
    }

    public void refund(UUID orderId) {
        charges.remove(orderId);
    }

    public BigDecimal chargedAmount(UUID orderId) {
        return charges.getOrDefault(orderId, BigDecimal.ZERO);
    }

    public BigDecimal totalCharged() {
        return charges.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
