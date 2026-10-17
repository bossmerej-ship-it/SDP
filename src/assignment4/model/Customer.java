package assignment4.model;

public record Customer(String name, String email, String street, String city,
                       String countryCode, PaymentMethod paymentMethod) {
    public Customer {
        if (name == null || name.isBlank() || street == null || street.isBlank()
                || city == null || city.isBlank() || countryCode == null || countryCode.isBlank()) {
            throw new IllegalArgumentException("Customer name and complete delivery address are required.");
        }
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("A valid contact email is required.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("A payment method is required.");
        }
        countryCode = countryCode.strip().toUpperCase(java.util.Locale.ROOT);
    }
}
