# Assignment 4 - Facade Pattern

Java 17 implementation of **Option B: Online Order Processing** for Software Design Patterns.

Four independent subsystem services manage inventory, payments, shipping, and notifications.
The application uses in-memory data and simulated integrations; it never charges a real card or sends email.

## Run with JDK 17

No Maven, Gradle, or third-party Java libraries are needed. Set `JAVA_HOME` to your JDK 17
installation, or make sure `java` and `javac` on `PATH` belong to that JDK.

Windows PowerShell:

```powershell
./run.ps1 demo
./run.ps1 interactive
./run.ps1 test
```

Linux, macOS, or Git Bash:

```bash
bash run.sh demo
bash run.sh interactive
bash run.sh test
```

In IntelliJ IDEA, open this folder, select JDK 17, mark `src` as Sources Root, and run
`assignment4.Main`. Use program argument `--interactive` for the menu. Mark `tests` as Test
Sources Root to run the dependency-free `assignment4.OrderFacadeTest` main method.

## Public facade API

```java
OrderFacade shop = new OrderFacade();
Cart cart = new Cart(Map.of("BOOK", 1, "USB", 2));
Customer customer = new Customer("Demo Customer", "student@example.com",
        "10 Abay Street", "Kyzylorda", "KZ", PaymentMethod.APPROVED_CARD);
OrderReceipt receipt = shop.placeOrder(cart, customer);
OrderReceipt cancelled = shop.cancelOrder(receipt.orderId());
```

The third public operation, `listProducts()`, returns immutable catalog snapshots. `Main`
creates no subsystem objects and calls no subsystem methods. Services remain public and
independently usable by advanced clients; the test suite demonstrates this explicitly.

## Workflow and failure behavior

1. Inventory calculates the total and atomically reserves the complete cart.
2. The payment processor records the charge using `BigDecimal` amounts in KZT.
3. Shipping schedules delivery and generates a tracking number.
4. Notifications format a message and append it to a simulated local outbox.

A rejected payment releases stock. A shipping failure refunds the charge and releases stock.
Cancellation cancels delivery, refunds payment, and releases stock. Repeated cancellation
returns the original cancellation receipt without repeating side effects. A notification
outage leaves the transaction successful and sets `notificationDelivered=false` on the receipt.
The cause and any suppressed compensation failures are preserved in `OrderException`.

The demo covers successful checkout, cancellation, repeated cancellation, declined payment,
unsupported delivery, and insufficient stock. The initial catalog has 5 BOOK, 3 MOUSE, and
10 USB units. One BOOK plus two USB drives costs **19,500.00 KZT**. Declined cards use
`DECLINED_CARD`; shipping supports `KZ`, so `US` demonstrates a later workflow failure.

## Structure and submission materials

```text
src/assignment4/
  Main.java                  Client and console demonstration
  OrderFacade.java           Three-operation public facade
  OrderException.java        Workflow failure and preserved diagnostics
  model/                     Immutable domain values and enums
  service/                   Four independent stateful subsystem classes
tests/assignment4/           19 dependency-free behavioral tests
docs/REPORT.pdf             Report for Moodle with UML and six Clean Code principles
docs/UML.png                Rendered UML class diagram
docs/UML.svg                Vector version of the UML diagram
docs/UML.puml               Editable PlantUML source
docs/DEFENSE.md             Russian walkthrough and questions for preparation
tools/build_report.py       Reproducible report and diagram generator
```

Validation: JDK 17, `--release 17 -Xlint:all -Werror`; all 19 behavioral tests pass. The
tests cover totals, rollback, cancellation, immutable snapshots, decimal precision,
notification failure, compensation diagnostics, and direct subsystem use.

Scope: a single-threaded educational simulation with no database, real payments, courier
API, or email. State resets at restart. Stock remains reserved for an active order because
fulfilment is outside this assignment. External failures need durable recovery in a real system;
the demo preserves diagnostics but has no background recovery process.

The [supplied smart-home example](https://github.com/252434-student/SDP/tree/main/src/assignment4)
was reviewed as a structural reference. This project implements the assignment's approved
Option B with independently written code.

Repository: <https://github.com/bossmerej-ship-it/SDP>
