package assignment4;

public class PaymentProcessor {
    private int paidAmount = 0;

    public boolean pay(int amount, boolean approved) {
        if (!approved || amount <= 0 || paidAmount > 0) {
            System.out.println("Payment declined or a payment already exists.");
            return false;
        }
        paidAmount = amount;
        System.out.println("Payment accepted: " + amount + " KZT");
        return true;
    }

    public void refund() {
        if (paidAmount > 0) {
            System.out.println("Refunded: " + paidAmount + " KZT");
            paidAmount = 0;
        }
    }

    public int getPaidAmount() {
        return paidAmount;
    }
}
