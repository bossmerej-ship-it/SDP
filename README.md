# Assignment 4 - Facade Pattern

Java 17 implementation of **Option B: Online Order Processing** for Software Design Patterns.

Four independent subsystem services manage inventory, payments, shipping, and notifications.
The facade and executable client will provide a single entry point for placing and cancelling orders.
The application uses in-memory data and simulated integrations; it never charges a real card or sends email.

Source structure: `src/assignment4/model` contains immutable domain values and
`src/assignment4/service` contains the independent subsystem services.
