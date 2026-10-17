package assignment4;

public class NotificationService {
    private String lastMessage = "No notifications yet.";

    public void orderPlaced(int quantity, int total, String city) {
        lastMessage = "Order placed: " + quantity + " book(s), " + total
                + " KZT. Delivery to " + city + ".";
        System.out.println("Notification: " + lastMessage);
    }

    public void orderCancelled() {
        lastMessage = "Order cancelled. Your payment and books have been returned.";
        System.out.println("Notification: " + lastMessage);
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
