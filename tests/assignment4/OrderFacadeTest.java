package assignment4;

import assignment4.model.*;
import assignment4.service.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dependency-free behavioral tests. Assertions always run, without requiring -ea. */
public final class OrderFacadeTest {
    private static int passed;

    private OrderFacadeTest() { }

    public static void main(String[] args) {
        run("successful checkout coordinates all four services", OrderFacadeTest::successfulCheckout);
        run("declined payment releases stock", OrderFacadeTest::declinedPayment);
        run("failed shipping refunds payment and releases stock", OrderFacadeTest::failedShipping);
        run("multi-item reservation is atomic", OrderFacadeTest::insufficientStock);
        run("unknown SKU leaves no partial work", OrderFacadeTest::unknownProduct);
        run("invalid carts are rejected", OrderFacadeTest::invalidCart);
        run("invalid customer details are rejected", OrderFacadeTest::invalidCustomer);
        run("cancellation reverses all business effects", OrderFacadeTest::cancellation);
        run("repeated cancellation is idempotent", OrderFacadeTest::repeatedCancellation);
        run("unknown cancellation does not affect other orders", OrderFacadeTest::unknownOrder);
        run("independent orders retain separate state", OrderFacadeTest::independentOrders);
        run("cart and catalog snapshots are immutable", OrderFacadeTest::immutableValues);
        run("money calculations use exact decimals", OrderFacadeTest::decimalMoney);
        run("notification failure preserves successful business state", OrderFacadeTest::notificationFailure);
        run("partial cancellation can be retried safely", OrderFacadeTest::retryCancellation);
        run("all compensations run and preserve failure diagnostics", OrderFacadeTest::compensationFailure);
        run("subsystems can be used without a facade", OrderFacadeTest::directSubsystemUse);
        run("duplicate operations cannot double reserve or double charge", OrderFacadeTest::duplicateOperations);
        run("normal client API hides subsystem wiring", OrderFacadeTest::publicApi);
        System.out.println("\nAll " + passed + " tests passed.");
    }

    private static void successfulCheckout() {
        Fixture f = fixture();
        OrderReceipt receipt = f.shop.placeOrder(new Cart(Map.of("BOOK", 1, "USB", 2)), customer());
        money("19500.00", receipt.total());
        equal(OrderStatus.PLACED, receipt.status());
        equal(4, stock(f.shop, "BOOK"));
        equal(8, stock(f.shop, "USB"));
        money("19500.00", f.payments.chargedAmount(receipt.orderId()));
        check(f.shipping.isScheduled(receipt.orderId()), "shipment missing");
        check(receipt.trackingNumber().startsWith("KZ-"), "tracking number missing");
        check(receipt.notificationDelivered(), "notification missing");
        equal(1, f.notifications.messages().size());
    }

    private static void declinedPayment() {
        Fixture f = fixture();
        OrderException failure = expect(OrderException.class,
                () -> f.shop.placeOrder(cart(), customer(PaymentMethod.DECLINED_CARD, "KZ")));
        check(failure.getCause() instanceof IllegalStateException, "original cause lost");
        untouched(f);
    }

    private static void failedShipping() {
        Fixture f = fixture();
        expect(OrderException.class,
                () -> f.shop.placeOrder(cart(), customer(PaymentMethod.APPROVED_CARD, "US")));
        untouched(f);
    }

    private static void insufficientStock() {
        Fixture f = fixture();
        expect(OrderException.class,
                () -> f.shop.placeOrder(new Cart(Map.of("BOOK", 1, "USB", 100)), customer()));
        untouched(f);
    }

    private static void unknownProduct() {
        Fixture f = fixture();
        expect(OrderException.class,
                () -> f.shop.placeOrder(new Cart(Map.of("BOOK", 1, "UNKNOWN", 1)), customer()));
        untouched(f);
    }

    private static void invalidCart() {
        expect(IllegalArgumentException.class, () -> new Cart(Map.of()));
        expect(IllegalArgumentException.class, () -> new Cart(null));
        expect(IllegalArgumentException.class, () -> new Cart(Map.of("BOOK", 0)));
        expect(IllegalArgumentException.class, () -> new Cart(Map.of("BOOK", -1)));
        expect(IllegalArgumentException.class, () -> new Cart(Map.of(" ", 1)));
        Fixture f = fixture();
        expect(IllegalArgumentException.class, () -> f.shop.placeOrder(null, customer()));
        expect(IllegalArgumentException.class, () -> f.shop.placeOrder(cart(), null));
        untouched(f);
    }

    private static void invalidCustomer() {
        expect(IllegalArgumentException.class, () -> new Customer("", "a@example.com", "10 Abay",
                "Kyzylorda", "KZ", PaymentMethod.APPROVED_CARD));
        expect(IllegalArgumentException.class, () -> new Customer("A", "invalid", "10 Abay",
                "Kyzylorda", "KZ", PaymentMethod.APPROVED_CARD));
        expect(IllegalArgumentException.class, () -> new Customer("A", "a@example.com", "",
                "Kyzylorda", "KZ", PaymentMethod.APPROVED_CARD));
        expect(IllegalArgumentException.class, () -> new Customer("A", "a@example.com", "10 Abay",
                "Kyzylorda", "KZ", null));
        equal("KZ", customer(PaymentMethod.APPROVED_CARD, " kz ").countryCode());
    }

    private static void cancellation() {
        Fixture f = fixture();
        OrderReceipt placed = f.shop.placeOrder(cart(), customer());
        OrderReceipt cancelled = f.shop.cancelOrder(placed.orderId());
        equal(OrderStatus.CANCELLED, cancelled.status());
        equal(5, stock(f.shop, "BOOK"));
        check(!f.inventory.hasReservation(placed.orderId()), "reservation still active");
        money("0", f.payments.chargedAmount(placed.orderId()));
        check(!f.shipping.isScheduled(placed.orderId()), "shipment still active");
        equal(2, f.notifications.messages().size());
    }

    private static void repeatedCancellation() {
        Fixture f = fixture();
        OrderReceipt placed = f.shop.placeOrder(cart(), customer());
        OrderReceipt cancelled = f.shop.cancelOrder(placed.orderId());
        equal(cancelled, f.shop.cancelOrder(placed.orderId()));
        equal(5, stock(f.shop, "BOOK"));
        money("0", f.payments.totalCharged());
        equal(2, f.notifications.messages().size());
    }

    private static void unknownOrder() {
        Fixture f = fixture();
        OrderReceipt placed = f.shop.placeOrder(cart(), customer());
        expect(OrderException.class, () -> f.shop.cancelOrder(UUID.randomUUID()));
        equal(4, stock(f.shop, "BOOK"));
        money("12500", f.payments.totalCharged());
        check(f.shipping.isScheduled(placed.orderId()), "other order was changed");
    }

    private static void independentOrders() {
        Fixture f = fixture();
        OrderReceipt first = f.shop.placeOrder(cart(), customer());
        OrderReceipt second = f.shop.placeOrder(cart(), customer());
        check(!first.orderId().equals(second.orderId()), "order IDs must differ");
        f.shop.cancelOrder(first.orderId());
        equal(4, stock(f.shop, "BOOK"));
        money("12500", f.payments.chargedAmount(second.orderId()));
        equal(1, f.shipping.activeShipmentCount());
    }

    private static void immutableValues() {
        Map<String, Integer> source = new HashMap<>(Map.of("BOOK", 1));
        Cart cart = new Cart(source);
        source.put("BOOK", 99);
        equal(1, cart.items().get("BOOK"));
        expect(UnsupportedOperationException.class, () -> cart.items().put("USB", 1));
        Fixture f = fixture();
        List<Product> snapshot = f.shop.listProducts();
        expect(UnsupportedOperationException.class, snapshot::clear);
        f.shop.placeOrder(cart, customer());
        equal(5, snapshot.stream().filter(p -> p.sku().equals("BOOK")).findFirst().orElseThrow()
                .availableQuantity());
    }

    private static void decimalMoney() {
        InventoryService inventory = new InventoryService(List.of(
                new Product("A", "A", new BigDecimal("0.10"), 5),
                new Product("B", "B", new BigDecimal("0.20"), 5)));
        OrderFacade shop = new OrderFacade(inventory, new PaymentProcessor(),
                new ShippingService(), new NotificationService());
        money("0.50", shop.placeOrder(new Cart(Map.of("A", 3, "B", 1)), customer()).total());
    }

    private static void notificationFailure() {
        Fixture f = fixture(new PaymentProcessor(), new ShippingService(), new NotificationService() {
            @Override public void send(Customer customer, UUID id, OrderStatus status) {
                throw new IllegalStateException("Outbox unavailable");
            }
        });
        OrderReceipt placed = f.shop.placeOrder(cart(), customer());
        check(!placed.notificationDelivered(), "outage must be visible");
        check(f.shipping.isScheduled(placed.orderId()), "business success was undone");
        money("12500", f.payments.totalCharged());
        OrderReceipt cancelled = f.shop.cancelOrder(placed.orderId());
        equal(OrderStatus.CANCELLED, cancelled.status());
        check(!cancelled.notificationDelivered(), "cancellation outage must be visible");
        equal(5, stock(f.shop, "BOOK"));
        money("0", f.payments.totalCharged());
    }

    private static void retryCancellation() {
        PaymentProcessor flaky = new PaymentProcessor() {
            private boolean failOnce = true;
            @Override public void refund(UUID id) {
                if (failOnce) {
                    failOnce = false;
                    throw new IllegalStateException("Temporary refund outage");
                }
                super.refund(id);
            }
        };
        Fixture f = fixture(flaky, new ShippingService(), new NotificationService());
        OrderReceipt placed = f.shop.placeOrder(cart(), customer());
        OrderException failure = expect(OrderException.class, () -> f.shop.cancelOrder(placed.orderId()));
        equal(1, failure.getSuppressed().length);
        equal(5, stock(f.shop, "BOOK"));
        money("12500", f.payments.totalCharged());
        equal(1, f.notifications.messages().size());
        equal(OrderStatus.CANCELLED, f.shop.cancelOrder(placed.orderId()).status());
        equal(5, stock(f.shop, "BOOK"));
        money("0", f.payments.totalCharged());
    }

    private static void compensationFailure() {
        ShippingService broken = new ShippingService() {
            @Override public String schedule(UUID id, Customer customer) {
                throw new IllegalStateException("Courier unavailable");
            }
            @Override public void cancel(UUID id) {
                throw new IllegalStateException("Courier cleanup unavailable");
            }
        };
        Fixture f = fixture(new PaymentProcessor(), broken, new NotificationService());
        OrderException failure = expect(OrderException.class, () -> f.shop.placeOrder(cart(), customer()));
        equal("Courier unavailable", failure.getCause().getMessage());
        equal(1, failure.getSuppressed().length);
        equal("Courier cleanup unavailable", failure.getSuppressed()[0].getMessage());
        equal(5, stock(f.shop, "BOOK"));
        money("0", f.payments.totalCharged());
    }

    private static void directSubsystemUse() {
        InventoryService inventory = new InventoryService(products());
        PaymentProcessor payments = new PaymentProcessor();
        ShippingService shipping = new ShippingService();
        NotificationService notifications = new NotificationService();
        UUID id = UUID.randomUUID();
        inventory.reserve(id, cart());
        payments.charge(id, inventory.calculateTotal(cart()), PaymentMethod.APPROVED_CARD);
        shipping.schedule(id, customer());
        notifications.send(customer(), id, OrderStatus.PLACED);
        check(inventory.hasReservation(id) && shipping.isScheduled(id), "direct use failed");
        equal(1, notifications.messages().size());
    }

    private static void duplicateOperations() {
        InventoryService inventory = new InventoryService(products());
        PaymentProcessor payments = new PaymentProcessor();
        UUID id = UUID.randomUUID();
        inventory.reserve(id, cart());
        expect(IllegalStateException.class, () -> inventory.reserve(id, cart()));
        payments.charge(id, new BigDecimal("12500"), PaymentMethod.APPROVED_CARD);
        expect(IllegalStateException.class,
                () -> payments.charge(id, new BigDecimal("12500"), PaymentMethod.APPROVED_CARD));
        inventory.release(id);
        inventory.release(id);
        equal(5, inventory.listProducts().get(0).availableQuantity());
        payments.refund(id);
        payments.refund(id);
        money("0", payments.totalCharged());
    }

    private static void publicApi() {
        equal(1, OrderFacade.class.getConstructors().length);
        equal(0, OrderFacade.class.getConstructors()[0].getParameterCount());
        List<String> methods = new ArrayList<>();
        for (var method : OrderFacade.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                methods.add(method.getName());
                check(!method.getReturnType().getPackageName().equals("assignment4.service"),
                        "subsystem type leaked");
            }
        }
        methods.sort(String::compareTo);
        equal(List.of("cancelOrder", "listProducts", "placeOrder"), methods);
    }

    private static void untouched(Fixture f) {
        equal(5, stock(f.shop, "BOOK"));
        equal(10, stock(f.shop, "USB"));
        money("0", f.payments.totalCharged());
        equal(0, f.shipping.activeShipmentCount());
        equal(0, f.notifications.messages().size());
    }

    private static Fixture fixture() {
        return fixture(new PaymentProcessor(), new ShippingService(), new NotificationService());
    }

    private static Fixture fixture(PaymentProcessor payments, ShippingService shipping,
                                   NotificationService notifications) {
        InventoryService inventory = new InventoryService(products());
        return new Fixture(inventory, payments, shipping, notifications,
                new OrderFacade(inventory, payments, shipping, notifications));
    }

    private static List<Product> products() {
        return List.of(new Product("BOOK", "Book", new BigDecimal("12500.00"), 5),
                new Product("USB", "USB Drive", new BigDecimal("3500.00"), 10));
    }

    private static Cart cart() { return new Cart(Map.of("BOOK", 1)); }
    private static Customer customer() { return customer(PaymentMethod.APPROVED_CARD, "KZ"); }
    private static Customer customer(PaymentMethod method, String country) {
        return new Customer("Test Customer", "test@example.com", "10 Abay Street",
                "Kyzylorda", country, method);
    }
    private static int stock(OrderFacade shop, String sku) {
        return shop.listProducts().stream().filter(p -> p.sku().equals(sku))
                .findFirst().orElseThrow().availableQuantity();
    }
    private static void money(String expected, BigDecimal actual) {
        check(new BigDecimal(expected).compareTo(actual) == 0, "Expected " + expected + ", got " + actual);
    }
    private static void equal(Object expected, Object actual) {
        check(java.util.Objects.equals(expected, actual), "Expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            if (type.isInstance(failure)) { return type.cast(failure); }
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }
    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }
    private record Fixture(InventoryService inventory, PaymentProcessor payments,
                           ShippingService shipping, NotificationService notifications,
                           OrderFacade shop) { }
}
