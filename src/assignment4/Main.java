package assignment4;

import assignment4.model.Cart;
import assignment4.model.Customer;
import assignment4.model.OrderReceipt;
import assignment4.model.PaymentMethod;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/** The application client only uses OrderFacade and immutable domain values. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        OrderFacade shop = new OrderFacade();
        if (args.length > 0 && "--interactive".equals(args[0])) {
            runInteractive(shop);
        } else {
            runDemo(shop);
        }
    }

    private static void runDemo(OrderFacade shop) {
        System.out.println("ASSIGNMENT 4 - ONLINE ORDER FACADE\n");
        showCatalog(shop);
        System.out.println("\n1. Successful multi-item checkout");
        Cart cart = new Cart(Map.of("BOOK", 1, "USB", 2));
        OrderReceipt receipt = shop.placeOrder(cart, customer(PaymentMethod.APPROVED_CARD, "KZ"));
        showReceipt(receipt);
        showCatalog(shop);

        System.out.println("\n2. Cancellation and repeated cancellation");
        showReceipt(shop.cancelOrder(receipt.orderId()));
        showReceipt(shop.cancelOrder(receipt.orderId()));
        showCatalog(shop);

        System.out.println("\n3. Declined payment: reserved stock is released");
        demonstrateFailure(shop, cart, customer(PaymentMethod.DECLINED_CARD, "KZ"));
        System.out.println("\n4. Unsupported delivery: payment and stock are restored");
        demonstrateFailure(shop, cart, customer(PaymentMethod.APPROVED_CARD, "US"));
        System.out.println("\n5. Insufficient stock: the entire cart is rejected");
        demonstrateFailure(shop, new Cart(Map.of("BOOK", 1, "MOUSE", 99)),
                customer(PaymentMethod.APPROVED_CARD, "KZ"));
        System.out.println("\nRun with --interactive for the console menu.");
    }

    private static void demonstrateFailure(OrderFacade shop, Cart cart, Customer customer) {
        try {
            shop.placeOrder(cart, customer);
            System.out.println("Unexpected success.");
        } catch (OrderException failure) {
            System.out.println(failure.getMessage());
        }
        showCatalog(shop);
    }

    private static void runInteractive(OrderFacade shop) {
        UUID lastOrderId = null;
        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n1 - Catalog | 2 - Place order | 3 - Cancel last order | 0 - Exit");
                if (!input.hasNextLine()) {
                    return;
                }
                String choice = input.nextLine().strip();
                try {
                    switch (choice) {
                        case "1" -> showCatalog(shop);
                        case "2" -> {
                            System.out.print("SKU (BOOK, MOUSE, USB): ");
                            String sku = input.nextLine().strip().toUpperCase(java.util.Locale.ROOT);
                            System.out.print("Quantity: ");
                            int quantity = Integer.parseInt(input.nextLine().strip());
                            System.out.print("Payment (1 = approved, 2 = declined): ");
                            String paymentChoice = input.nextLine().strip();
                            if (!paymentChoice.equals("1") && !paymentChoice.equals("2")) {
                                throw new IllegalArgumentException("Choose payment 1 or 2.");
                            }
                            PaymentMethod method = paymentChoice.equals("2")
                                    ? PaymentMethod.DECLINED_CARD : PaymentMethod.APPROVED_CARD;
                            System.out.print("Delivery country code (KZ supported): ");
                            String country = input.nextLine().strip();
                            OrderReceipt receipt = shop.placeOrder(new Cart(Map.of(sku, quantity)),
                                    customer(method, country));
                            lastOrderId = receipt.orderId();
                            showReceipt(receipt);
                        }
                        case "3" -> {
                            if (lastOrderId == null) {
                                System.out.println("Place an order first.");
                            } else {
                                showReceipt(shop.cancelOrder(lastOrderId));
                            }
                        }
                        case "0" -> { return; }
                        default -> System.out.println("Choose 0, 1, 2, or 3.");
                    }
                } catch (IllegalArgumentException | OrderException failure) {
                    System.out.println("Error: " + failure.getMessage());
                } catch (java.util.NoSuchElementException endOfInput) {
                    return;
                }
            }
        }
    }

    private static Customer customer(PaymentMethod method, String country) {
        return new Customer("Demo Customer", "student@example.com", "10 Abay Street",
                "Kyzylorda", country, method);
    }

    private static void showCatalog(OrderFacade shop) {
        System.out.println("Catalog (KZT):");
        shop.listProducts().forEach(product -> System.out.printf("  %s | %s | %s | stock=%d%n",
                product.sku(), product.name(), product.unitPrice(), product.availableQuantity()));
    }

    private static void showReceipt(OrderReceipt receipt) {
        System.out.printf("Order %s | %s | %s KZT | tracking=%s | notified=%s%n",
                receipt.orderId(), receipt.status(), receipt.total(),
                receipt.trackingNumber(), receipt.notificationDelivered());
    }
}
