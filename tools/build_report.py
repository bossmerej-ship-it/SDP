"""Rebuild the report and UML assets. Requires reportlab and Poppler, not the Java application.

Usage: python tools/build_report.py
"""
from pathlib import Path
import subprocess

from reportlab.graphics import renderPDF, renderSVG
from reportlab.graphics.shapes import Drawing, Line, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image, KeepTogether, PageBreak, Paragraph, Preformatted, SimpleDocTemplate,
)

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
SCRATCH = ROOT / "build" / "report"
REPO = "https://github.com/bossmerej-ship-it/SDP"
INK = colors.HexColor("#19232D")
BLUE = colors.HexColor("#1F4F70")
PALE = colors.HexColor("#EDF3F7")


def register_fonts():
    candidates = [
        (Path("C:/Windows/Fonts/arial.ttf"), Path("C:/Windows/Fonts/arialbd.ttf"),
         Path("C:/Windows/Fonts/consola.ttf")),
        (Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
         Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"),
         Path("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf")),
    ]
    for regular, bold, mono in candidates:
        if all(p.exists() for p in (regular, bold, mono)):
            pdfmetrics.registerFont(TTFont("Body", str(regular)))
            pdfmetrics.registerFont(TTFont("BodyBold", str(bold)))
            pdfmetrics.registerFont(TTFont("Code", str(mono)))
            pdfmetrics.registerFontFamily("Body", normal="Body", bold="BodyBold",
                                         italic="Body", boldItalic="BodyBold")
            return
    raise RuntimeError("Install Arial/Consolas or DejaVu fonts to rebuild the report.")


def uml():
    d = Drawing(960, 980)
    d.add(Rect(0, 0, 960, 980, fillColor=colors.white, strokeColor=None))

    def text(x, y, value, size=14, bold=False, center=False):
        d.add(String(x, y, value, fontName="BodyBold" if bold else "Body",
                     fontSize=size, fillColor=INK, textAnchor="middle" if center else "start"))

    def box(x, y, w, h, stereotype, name, attributes, methods):
        d.add(Rect(x, y, w, h, fillColor=colors.white, strokeColor=BLUE, strokeWidth=1.8))
        d.add(Rect(x, y + h - 53, w, 53, fillColor=PALE, strokeColor=None))
        text(x + w / 2, y + h - 20, stereotype, 12, center=True)
        text(x + w / 2, y + h - 42, name, 17, True, True)
        d.add(Line(x, y + h - 53, x + w, y + h - 53, strokeColor=BLUE))
        cursor = y + h - 76
        for value in attributes:
            text(x + 12, cursor, value, 13)
            cursor -= 21
        if attributes:
            d.add(Line(x, cursor + 7, x + w, cursor + 7, strokeColor=BLUE))
            cursor -= 15
        for value in methods:
            text(x + 12, cursor, value, 13)
            cursor -= 21

    def arrow(points, dashed=False):
        for (x1, y1), (x2, y2) in zip(points, points[1:]):
            line = Line(x1, y1, x2, y2, strokeColor=BLUE, strokeWidth=1.8)
            if dashed:
                line.strokeDashArray = [6, 4]
            d.add(line)
        (x1, y1), (x2, y2) = points[-2:]
        import math
        angle = math.atan2(y2 - y1, x2 - x1)
        for offset in (-0.5, 0.5):
            d.add(Line(x2, y2, x2 - 12 * math.cos(angle + offset),
                       y2 - 12 * math.sin(angle + offset), strokeColor=BLUE, strokeWidth=1.8))

    box(330, 857, 300, 96, "<<Client>>", "Main", [], ["+ main(args: String[]): void {static}"])
    box(215, 577, 530, 252, "<<Facade>>", "OrderFacade", [
        "- inventory / payments / shipping / notifications",
        "- orders: Map<UUID, Order>",
    ], [
        "+ OrderFacade()",
        "+ listProducts(): List<Product>",
        "+ placeOrder(Cart, Customer): OrderReceipt",
        "+ cancelOrder(UUID): OrderReceipt",
    ])
    box(30, 278, 435, 239, "<<Subsystem>>", "InventoryService", [
        "- catalog: Map<String, Product>",
        "- stock: Map<String, Integer>",
        "- reservations: Map<UUID, Cart>",
    ], [
        "+ calculateTotal(Cart): BigDecimal",
        "+ reserve(UUID, Cart): void",
        "+ release(UUID): void",
        "+ listProducts(): List<Product>",
    ])
    box(495, 278, 435, 239, "<<Subsystem>>", "PaymentProcessor", [
        "- charges: Map<UUID, BigDecimal>",
    ], [
        "+ charge(UUID, BigDecimal, PaymentMethod): void",
        "+ refund(UUID): void",
        "+ chargedAmount(UUID): BigDecimal",
        "+ totalCharged(): BigDecimal",
    ])
    box(30, 55, 435, 183, "<<Subsystem>>", "ShippingService", [
        "- shipments: Map<UUID, String>",
    ], [
        "+ schedule(UUID, Customer): String",
        "+ cancel(UUID): void",
        "+ isScheduled(UUID): boolean",
    ])
    box(495, 55, 435, 183, "<<Subsystem>>", "NotificationService", [
        "- outbox: List<String>",
    ], [
        "+ send(Customer, UUID, OrderStatus): void",
        "+ messages(): List<String>",
    ])
    arrow([(480, 857), (480, 829)], True)
    text(491, 839, "uses", 12)
    arrow([(350, 577), (350, 548), (247, 548), (247, 517)])
    arrow([(610, 577), (610, 548), (712, 548), (712, 517)])
    arrow([(215, 680), (12, 680), (12, 255), (247, 255), (247, 238)])
    arrow([(745, 680), (948, 680), (948, 255), (712, 255), (712, 238)])
    text(480, 20, "Solid arrows: one-way service associations. Dashed arrow: client dependency.",
         13, center=True)
    return d


def main():
    DOCS.mkdir(exist_ok=True)
    SCRATCH.mkdir(parents=True, exist_ok=True)
    register_fonts()
    diagram = uml()
    renderSVG.drawToFile(diagram, str(DOCS / "UML.svg"))
    renderPDF.drawToFile(diagram, str(SCRATCH / "uml.pdf"))
    subprocess.run(["pdftoppm", "-singlefile", "-scale-to", "2200", "-png",
                    str(SCRATCH / "uml.pdf"), str(DOCS / "UML")], check=True)

    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle("ReportTitle", fontName="BodyBold", fontSize=24,
                              leading=29, textColor=colors.black, spaceAfter=12))
    styles.add(ParagraphStyle("Section", fontName="BodyBold", fontSize=15,
                              leading=19, textColor=colors.black, spaceBefore=12, spaceAfter=7))
    styles.add(ParagraphStyle("Subsection", fontName="BodyBold", fontSize=11.5,
                              leading=15, textColor=colors.black, spaceBefore=10, spaceAfter=5))
    styles.add(ParagraphStyle("Text", fontName="Body", fontSize=10.4, leading=15,
                              textColor=INK, spaceAfter=8))
    styles.add(ParagraphStyle("SmallText", parent=styles["Text"], fontSize=9, leading=12))
    styles.add(ParagraphStyle("CodeBlock", fontName="Code", fontSize=8.3, leading=12,
                              textColor=INK, backColor=PALE, borderPadding=9,
                              spaceBefore=5, spaceAfter=12))
    styles.add(ParagraphStyle("Caption", parent=styles["SmallText"], alignment=TA_CENTER))

    story = []

    def p(text, style="Text"):
        story.append(Paragraph(text, styles[style]))

    def h(text):
        p(text, "Section")

    def code(text):
        story.append(Preformatted(text, styles["CodeBlock"]))

    def principle(title, body, excerpt, annotation):
        parts = [Paragraph(title, styles["Subsection"]), Paragraph(body, styles["Text"]),
                 Preformatted(excerpt, styles["CodeBlock"]),
                 Paragraph(annotation, styles["SmallText"])]
        story.append(KeepTogether(parts))

    p("Online Order Processing<br/>with the Facade Pattern", "ReportTitle")
    p("Assignment 4 | Software Design Patterns | Java 17", "Subsection")
    p("Course ShP-2216 | Option B | 17 October 2026<br/>"
      "Prepared for repository owner <b>bossmerej-ship-it</b>", "SmallText")
    h("1 Introduction")
    p("An online checkout is one user action with several dependent steps. Stock must be "
      "available, payment must succeed, and a courier must accept delivery before the order can "
      "be confirmed. If each client implements this sequence, it must also implement the matching "
      "rollback rules. The same coordination would be repeated in a console, website, or mobile app.")
    p("This project implements the assignment's suggested Option B. <b>OrderFacade</b> exposes "
      "<b>placeOrder(cart, customer)</b>, <b>cancelOrder(orderId)</b>, and <b>listProducts()</b>. "
      "The client supplies domain data and receives a receipt; it does not build or call any of "
      "the four subsystem services. The facade provides a simpler entry point to a complex "
      "workflow, following the Facade structure described by Refactoring.Guru [1].")
    h("The subsystem performs observable work")
    p("<b>InventoryService</b> calculates totals from a catalog, validates all cart items, updates "
      "stock, and records reservations. <b>PaymentProcessor</b> records decimal charges and "
      "refunds. <b>ShippingService</b> validates courier coverage and records tracking numbers. "
      "<b>NotificationService</b> formats messages and stores them in a local outbox.")
    p("The services collaborate through the facade using the same order UUID, cart, calculated "
      "amount, and customer. They are stateful simulations rather than external integrations. "
      "There is no real card charge, email delivery, or courier request.")
    h("A simpler client")
    code('OrderFacade shop = new OrderFacade();\n'
         'OrderReceipt receipt = shop.placeOrder(cart, customer);\n'
         'OrderReceipt cancelled = shop.cancelOrder(receipt.orderId());')
    p("Without the facade, a client must price the cart, reserve inventory, charge payment, "
      "schedule shipping, notify the customer, and compensate earlier steps on failure. "
      "The three client calls above replace that orchestration, while the services remain "
      "available for advanced direct use.")

    story.append(PageBreak())
    h("2 UML class diagram")
    p("The application client depends on the facade. The facade has one-way associations to "
      "four subsystem services. No service contains a reference to OrderFacade.")
    story.append(Image(str(DOCS / "UML.png"), width=155 * mm, height=158.23 * mm))
    p("Figure 1. Core classes, private state, and principal public operations. "
      "+ means public and - means private. Selected accessors are omitted for readability.", "Caption")
    h("Data contracts and independence")
    p("Cart, Customer, Product, and OrderReceipt are Java records; PaymentMethod and OrderStatus "
      "are enums. These domain values cross the facade boundary. OrderException preserves "
      "workflow failure diagnostics. The complete editable diagram, including domain "
      "dependencies and helper methods, is provided in docs/UML.puml.")
    p("The public no-argument constructor creates the default subsystem. A package-private "
      "constructor allows tests to inject controlled failures. Each service has public methods "
      "and can be instantiated without a facade; the directSubsystemUse test verifies this.")

    story.append(PageBreak())
    h("3 Clean Code principles")
    p("The following six principles are illustrated with short excerpts from the implementation. "
      "The annotations explain why each choice helps this workflow.")
    principle("1 Meaningful names and a small API",
              "The public methods describe business actions. The caller does not need to learn "
              "reservation IDs, gateway commands, or shipment construction.",
              'OrderReceipt receipt = shop.placeOrder(cart, customer);\n'
              'shop.cancelOrder(receipt.orderId());',
              "Annotated excerpt from Main: placeOrder and cancelOrder communicate intent; "
              "OrderReceipt names the returned business result. The only additional operation "
              "is the catalog query listProducts.")
    principle("2 Single Responsibility and focused services",
              "The facade coordinates. Inventory owns pricing and reservation rules, payments "
              "own charges, shipping owns coverage and tracking, and notifications own messages.",
              'total = inventory.calculateTotal(cart);\n'
              'inventory.reserve(orderId, cart);\n'
              'payments.charge(orderId, total, customer.paymentMethod());\n'
              'trackingNumber = shipping.schedule(orderId, customer);',
              "Annotated excerpt from OrderFacade: business steps are delegated in order. "
              "The facade does not subtract quantities, calculate line prices, or build a tracking number.")
    principle("3 Encapsulation and immutable boundaries",
              "The facade returns domain values rather than internal service objects. Input "
              "collections are copied so a caller cannot silently change an existing reservation.",
              '// Cart compact constructor, after validating every item:\n'
              'items = Map.copyOf(items);\n'
              '// OrderFacade stores its collaborators privately:\n'
              'private final InventoryService inventory;',
              "Annotated excerpts from Cart and OrderFacade: the defensive copy protects "
              "the cart from later changes to the caller's map. Private final service fields "
              "prevent normal clients from reaching the subsystem through the facade.")

    story.append(PageBreak())
    h("Clean Code principles continued")
    principle("4 Fail fast before changing shared state",
              "Inventory checks every item before subtracting any stock. A cart containing an "
              "available book and an unavailable USB drive must not reserve just the book.",
              'for (var item : cart.items().entrySet()) {\n'
              '    requireProduct(item.getKey());\n'
              '    if (stock.get(item.getKey()) < item.getValue()) {\n'
              '        throw new IllegalStateException(\n'
              '                "Insufficient stock for " + item.getKey());\n'
              '    }\n'
              '}',
              "Annotated excerpt from InventoryService.reserve: the entire validation loop "
              "finishes before the later subtraction pass. Cart and Customer also reject "
              "invalid values at construction.")
    principle("5 Explicit error handling with preserved diagnostics",
              "A later failure triggers reverse compensation. Cleanup must continue even if "
              "one compensating action throws, and the initial cause must remain inspectable.",
              'try {\n'
              '    action.run();\n'
              '} catch (RuntimeException rollbackFailure) {\n'
              '    failure.addSuppressed(rollbackFailure);\n'
              '}',
              "Annotated excerpt from OrderFacade.compensate: every action is attempted in "
              "shipping, payment, inventory order. The wrapper exception keeps the initial "
              "cause; suppressed exceptions retain cleanup errors. Notification failure instead "
              "sets a receipt flag after business success.")
    principle("6 Idempotent compensation and safe retries",
              "A repeated cancellation must not increase stock twice or refund twice. Services "
              "remove each recorded effect once; a second cleanup finds nothing to reverse.",
              'Cart cart = reservations.remove(orderId);\n'
              'if (cart != null) {\n'
              '    cart.items().forEach((sku, quantity) ->\n'
              '        stock.put(sku, stock.get(sku) + quantity));\n'
              '}',
              "Annotated excerpt from InventoryService.release: removing the reservation "
              "makes release idempotent. Payment refunds and shipment cancellation also remove "
              "records. The facade returns the stored receipt for an already cancelled order.")

    story.append(PageBreak())
    h("4 Validation and demonstration")
    p("The project was compiled and executed with JDK 17.0.12 using <b>--release 17 "
      "-Xlint:all -Werror</b>. All <b>19 behavioral tests</b> pass. Tests throw AssertionError "
      "on failure without requiring the JVM -ea option. No third-party Java library is required.")
    p("The automated checks cover successful multi-item checkout, declined payment, delivery "
      "failure, atomic reservation, unknown products, input validation, cancellation, repeated "
      "cancellation, separate orders, immutable snapshots, exact decimals, notification outage, "
      "cancellation retry, preserved cleanup errors, direct subsystem use, duplicate protection, "
      "and the simplified public API.")
    p("<b>Live sequence:</b> run <b>./run.ps1 demo</b> on Windows or <b>bash run.sh demo</b> "
      "on Linux/macOS. One BOOK at 12,500 KZT and two USB drives at 3,500 KZT total "
      "<b>19,500.00 KZT</b>. Stock moves from BOOK=5, USB=10 to BOOK=4, USB=8. "
      "Cancellation restores 5 and 10; repeating it leaves these values unchanged. "
      "The demo then exercises declined payment, unsupported delivery, and insufficient stock.")
    p("Use <b>./run.ps1 test</b> or <b>bash run.sh test</b> for the checks, and select "
      "<b>interactive</b> instead of <b>demo</b> to use the console menu. A declined card "
      "uses DECLINED_CARD. Country KZ succeeds; country US causes a shipping failure "
      "after payment, exercising both refund and stock release.")
    h("5 Conclusion and implementation limits")
    p("The facade hides subsystem creation, operation ordering, shared order identifiers, "
      "compensation, and notification policy. The client retains direct access to business "
      "data and receipts. Advanced clients may independently use any subsystem service; "
      "the services do not depend on the facade. This preserves the defining Facade property [1].")
    p("This is a single-threaded, in-memory simulation. Active orders retain reservations; "
      "fulfilment and completed delivery are outside scope. State resets when the process "
      "exits. Real deployments need persistence, concurrency control, integration adapters, "
      "and durable recovery for failed compensation. The simulation preserves those failure "
      "diagnostics but provides no background recovery or notification retry.")
    h("6 Repository and references")
    p(f'<b>GitHub repository:</b> <link href="{REPO}" color="#1F4F70">{REPO}</link>', "SmallText")
    p('[1] Refactoring.Guru. Facade. '
      '<link href="https://refactoring.guru/design-patterns/facade" color="#1F4F70">'
      'refactoring.guru/design-patterns/facade</link>. Accessed 17 October 2026.', "SmallText")
    p('[2] Oracle. Java SE 17 BigDecimal API and Record Classes. Decimal arithmetic and '
      'record semantics follow the Java 17 documentation: '
      '<link href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html" '
      'color="#1F4F70">BigDecimal</link>; '
      '<link href="https://docs.oracle.com/en/java/javase/17/language/records.html" '
      'color="#1F4F70">Record Classes</link>. Accessed 17 October 2026.', "SmallText")
    p('[3] Assignment_4_Facade_Pattern_Instructions.docx, supplied assignment brief. '
      'The supplied student repository was reviewed as a structural example; '
      'this project uses Option B and independently written code.', "SmallText")

    def footer(canvas, doc):
        canvas.saveState()
        canvas.setFont("Body", 8)
        canvas.setFillColor(colors.HexColor("#5A6873"))
        canvas.drawString(20 * mm, 13 * mm, "Assignment 4 | Online Order Processing Facade | Java 17")
        canvas.drawRightString(190 * mm, 13 * mm, str(doc.page))
        canvas.restoreState()

    doc = SimpleDocTemplate(str(DOCS / "REPORT.pdf"), pagesize=(210 * mm, 297 * mm),
                            rightMargin=20 * mm, leftMargin=20 * mm,
                            topMargin=17 * mm, bottomMargin=22 * mm,
                            title="Assignment 4 Online Order Processing Facade",
                            author="bossmerej-ship-it", subject="Facade Pattern and Clean Code")
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print("Built docs/REPORT.pdf and UML.svg/UML.png")


if __name__ == "__main__":
    main()
