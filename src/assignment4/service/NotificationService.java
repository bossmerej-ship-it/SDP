package assignment4.service;

import assignment4.model.Customer;
import assignment4.model.OrderStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Formats and stores a notification in a local outbox; no real email is sent. */
public class NotificationService {
    private final List<String> outbox = new ArrayList<>();

    public void send(Customer customer, UUID orderId, OrderStatus status) {
        String message = "To: " + customer.email() + " | Hello, " + customer.name()
                + ". Order " + orderId + " is " + status + ".";
        outbox.add(message);
    }

    public List<String> messages() {
        return List.copyOf(outbox);
    }
}
