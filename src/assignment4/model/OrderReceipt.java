package assignment4.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Client-facing result with no internal reservation, payment, or shipment objects. */
public record OrderReceipt(UUID orderId, BigDecimal total, OrderStatus status,
                           String trackingNumber, boolean notificationDelivered) {
}
