package assignment4;

public class InventoryService {
    private static final int BOOK_PRICE = 12500;
    private int stock = 5;
    private int reservedQuantity = 0;

    public boolean reserve(int quantity) {
        if (quantity <= 0 || quantity > stock || reservedQuantity > 0) {
            System.out.println("Invalid quantity, insufficient stock, or an existing reservation.");
            return false;
        }
        stock -= quantity;
        reservedQuantity = quantity;
        System.out.println("Reserved books: " + quantity);
        return true;
    }

    public int calculateTotal(int quantity) {
        if (quantity <= 0 || quantity > 5) {
            throw new IllegalArgumentException("Quantity must be from 1 to 5.");
        }
        return quantity * BOOK_PRICE;
    }

    public void release() {
        stock += reservedQuantity;
        reservedQuantity = 0;
        System.out.println("Reservation released.");
    }

    public int getStock() {
        return stock;
    }
}
