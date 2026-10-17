# Assignment 4 - Facade Pattern

**Topic B: Online Order Processing. Java 17.**

**Student:** Kuatbay Merey

**Group:** SE-2537

[GitHub repository](https://github.com/bossmerej-ship-it/SDP)

There are only six Java files:

- `src/Main.java` - console menu; uses only the facade.
- `src/assignment4/OrderFacade.java` - coordinates the four services.
- `InventoryService.java` - reserves books and calculates the price.
- `PaymentProcessor.java` - accepts payment and makes refunds.
- `ShippingService.java` - schedules and cancels delivery.
- `NotificationService.java` - prepares and stores the last notification.

Open the project in IntelliJ IDEA, select JDK 17, mark `src` as Sources Root,
and run `Main.java`. No libraries or scripts are needed.

The shop has 5 books at 12,500 KZT each and supports one active order at a time.
Place an order, cancel it, or show its status through the menu.
Payment option 2 demonstrates a declined payment. Delivery is available in
Kyzylorda, Almaty, and Astana; another city demonstrates a shipping failure.

If payment fails, the facade releases the books. If delivery fails, it refunds
payment and releases the books. Cancellation reverses all three steps.
All services are public and can also be used directly without the facade.
Payments, delivery, and notifications are simulated; data stays in memory.

`REPORT.pdf` contains the UML diagram and five annotated Clean Code examples.
