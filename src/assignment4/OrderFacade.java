package assignment4;

import assignment4.model.Cart;
import assignment4.model.Customer;
import assignment4.model.OrderReceipt;
import assignment4.model.OrderStatus;
import assignment4.model.Product;
import assignment4.service.InventoryService;
import assignment4.service.NotificationService;
import assignment4.service.PaymentProcessor;
import assignment4.service.ShippingService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Coordinates the subsystem. Domain values, rather than service objects, cross the public API. */
public class OrderFacade {
    private final InventoryService inventory;
    private final PaymentProcessor payments;
    private final ShippingService shipping;
    private final NotificationService notifications;
    private final Map<UUID, Order> orders = new HashMap<>();

    public OrderFacade() {
        this(new InventoryService(List.of(
                new Product("BOOK", "Design Patterns Book", new BigDecimal("12500.00"), 5),
                new Product("MOUSE", "Wireless Mouse", new BigDecimal("7500.00"), 3),
                new Product("USB", "USB Flash Drive", new BigDecimal("3500.00"), 10))),
                new PaymentProcessor(), new ShippingService(), new NotificationService());
    }

    // Package access allows controlled tests without exposing service wiring to normal clients.
    OrderFacade(InventoryService inventory, PaymentProcessor payments,
                ShippingService shipping, NotificationService notifications) {
        this.inventory = Objects.requireNonNull(inventory);
        this.payments = Objects.requireNonNull(payments);
        this.shipping = Objects.requireNonNull(shipping);
        this.notifications = Objects.requireNonNull(notifications);
    }

    public List<Product> listProducts() {
        return inventory.listProducts();
    }

    public OrderReceipt placeOrder(Cart cart, Customer customer) {
        if (cart == null || customer == null) {
            throw new IllegalArgumentException("Cart and customer are required.");
        }
        UUID orderId = UUID.randomUUID();
        BigDecimal total;
        String trackingNumber;
        try {
            total = inventory.calculateTotal(cart);
            inventory.reserve(orderId, cart);
            payments.charge(orderId, total, customer.paymentMethod());
            trackingNumber = shipping.schedule(orderId, customer);
        } catch (RuntimeException cause) {
            OrderException failure = new OrderException("Order could not be placed: "
                    + cause.getMessage(), cause);
            compensate(orderId, failure);
            throw failure;
        }

        boolean delivered = notifyCustomer(customer, orderId, OrderStatus.PLACED);
        OrderReceipt receipt = new OrderReceipt(orderId, total, OrderStatus.PLACED,
                trackingNumber, delivered);
        orders.put(orderId, new Order(customer, receipt));
        return receipt;
    }

    /** Repeated cancellation returns the same receipt without a second refund or stock release. */
    public OrderReceipt cancelOrder(UUID orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new OrderException("Unknown order: " + orderId, null);
        }
        if (order.receipt().status() == OrderStatus.CANCELLED) {
            return order.receipt();
        }

        OrderException failure = new OrderException("Cancellation could not finish. Retry order "
                + orderId + ".", null);
        compensate(orderId, failure);
        if (failure.getSuppressed().length > 0) {
            throw failure;
        }

        boolean delivered = notifyCustomer(order.customer(), orderId, OrderStatus.CANCELLED);
        OrderReceipt receipt = new OrderReceipt(orderId, order.receipt().total(),
                OrderStatus.CANCELLED, order.receipt().trackingNumber(), delivered);
        orders.put(orderId, new Order(order.customer(), receipt));
        return receipt;
    }

    /** Reverse workflow order. Every compensation is attempted even if an earlier one fails. */
    private void compensate(UUID orderId, OrderException failure) {
        List<Runnable> actions = List.of(() -> shipping.cancel(orderId),
                () -> payments.refund(orderId), () -> inventory.release(orderId));
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
        }
    }

    /** A notification outage must not undo an otherwise successful business transaction. */
    private boolean notifyCustomer(Customer customer, UUID orderId, OrderStatus status) {
        try {
            notifications.send(customer, orderId, status);
            return true;
        } catch (RuntimeException notificationFailure) {
            return false;
        }
    }

    private record Order(Customer customer, OrderReceipt receipt) { }
}
