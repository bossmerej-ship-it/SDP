import assignment4.OrderFacade;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        OrderFacade shop = new OrderFacade();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Place order");
            System.out.println("2. Cancel order");
            System.out.println("3. Show status");
            System.out.println("0. Exit");
            if (!scanner.hasNextLine()) {
                break;
            }
            String choice = scanner.nextLine();

            try {
                if (choice.equals("1")) {
                    System.out.print("Number of books (12500 KZT each): ");
                    int quantity = Integer.parseInt(scanner.nextLine());
                    System.out.print("Payment: 1 = approved, 2 = declined: ");
                    String payment = scanner.nextLine();
                    if (!payment.equals("1") && !payment.equals("2")) {
                        System.out.println("Choose payment 1 or 2.");
                        continue;
                    }
                    System.out.print("City (Kyzylorda, Almaty, Astana): ");
                    String city = scanner.nextLine();
                    shop.placeOrder(quantity, payment.equals("1"), city);
                } else if (choice.equals("2")) {
                    shop.cancelOrder();
                } else if (choice.equals("3")) {
                    shop.showStatus();
                } else if (choice.equals("0")) {
                    break;
                } else {
                    System.out.println("Choose 0, 1, 2, or 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            } catch (java.util.NoSuchElementException e) {
                break;
            }
        }
        scanner.close();
    }
}
