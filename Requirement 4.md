Practice 3 — Constructors & Static Factory (Tech Requirement)
Builds on: Task 1 (basic Product) & Task 2 (encapsulation + guards).
Theme: Learn constructor overloading, constructor chaining, default initialization, and simple static members / static factory methods.

Goal
Enhance product.Product to support multiple constructors and static factory helpers, while keeping validation simple and consistent with Task 2.

What to add (easy & minimal)
A) Constructors (overloaded + chained)
Implement three constructors for Product:
1.	No-args constructor — sets safe defaults:
o	id = "AUTO-" + nextSeq()
o	name = "Unnamed"
o	description = null
o	price = 0.0
o	quantity = 0
o	category = null (can be set later)
2.	Required-args constructor — id, name, price:
public Product(String id, String name, double price)
o	Sets quantity = 0, description = null, category = null.
3.	Full-args constructor — id, name, description, price, quantity, category.
Rules:
•	Use constructor chaining with this(...) to avoid code duplication.
•	Inside constructors, reuse your guards from Task 2: call trySetX methods.
If a guard returns false, keep the current value (default from the chained constructor).
B) Static members
Add these static members to Product:
•	public static final String DEFAULT_CURRENCY = "KZT";
•	private static int SEQ = 1; — simple product id sequence.
•	public static int getCreatedCount() — how many Product objects were created (increment in each constructor).
Helper:
private static String nextSeq() { return String.valueOf(SEQ++); }
C) Static factory methods (friendly names)
Add two static factory methods:
1.	public static Product of(String id, String name, double price)
o	Equivalent to the required-args constructor.
2.	public static Product freeSample(String name)
o	Creates a product with:
	auto id (AUTO-...),
	given name,
	price = 0.0,
	quantity = 1.
Tip: Static factories can have good names and can return preconfigured instances.

 Demo (product.ShopDemo or product.ShopDemo3)
Print results to show all flows:
1.	Create products via each constructor + the two static factories.
2.	Show automatically assigned ids (AUTO-1, AUTO-2, ...).
3.	Prove guards still work when constructor parameters are invalid (e.g., new Product("", "A", -5) should keep safe defaults for those fields).
4.	Print Product.getCreatedCount() at the end.
Example usage (you can copy into your demo):
Product p1 = new Product(); // no-args
Product p2 = new Product("P100", "Notebook", 950.0); // required-args
Product p3 = new Product("P200", "Headphones", "BT 5.0", 14990.0, 5, electronics); // full-args

Product p4 = Product.of("P300", "Pencil", 120.0); // static factory
Product p5 = Product.freeSample("Sticker");       // static factory

System.out.println(p1);
System.out.println(p2);
System.out.println(p3);
System.out.println(p4);
System.out.println(p5);

System.out.println("Created count = " + Product.getCreatedCount());

