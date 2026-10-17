package assignment4;

public class OrderFacade {
    private final InventoryService inventory = new InventoryService();
    private final PaymentProcessor payment = new PaymentProcessor();
    private final ShippingService shipping = new ShippingService();
    private final NotificationService notification = new NotificationService();
    private boolean orderActive = false;

    public void placeOrder(int quantity, boolean paymentApproved, String city) {
        if (orderActive) {
            System.out.println("Cancel the current order before placing another one.");
            return;
        }
        if (!inventory.reserve(quantity)) {
            return;
        }

        int total = inventory.calculateTotal(quantity);
        if (!payment.pay(total, paymentApproved)) {
            inventory.release(); // Payment failed: return reserved books.
            return;
        }
        if (!shipping.schedule(city)) {
            payment.refund(); // Shipping failed: undo payment and reservation.
            inventory.release();
            return;
        }

        orderActive = true;
        notification.orderPlaced(quantity, total, city.strip());
    }

    public void cancelOrder() {
        if (!orderActive) {
            System.out.println("There is no active order.");
            return;
        }
        shipping.cancel();
        payment.refund();
        inventory.release();
        orderActive = false;
        notification.orderCancelled();
    }

    public void showStatus() {
        System.out.println("Active order: " + orderActive);
        System.out.println("Books in stock: " + inventory.getStock());
        System.out.println("Paid: " + payment.getPaidAmount() + " KZT");
        System.out.println("Delivery: " + shipping.getCity());
    }
}
