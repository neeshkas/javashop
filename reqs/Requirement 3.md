Practice 2 — Encapsulation & Validation Guard (Tech Requirement)
Theme: Keep your objects in a valid state.
Builds on: Task 1 (Product, Category, ShopDemo).
Rule of thumb: No invalid data gets inside Product.
________________________________________
🎯 Goal
Refactor product.Product (and a tiny bit of category.Category) to enforce encapsulation and add simple validation guards.
No exceptions yet, no complex patterns. Just private fields + guarded updates that return boolean.
________________________________________
✅ What to change (minimal & easy)
1) Encapsulation
•	All fields in Product must be private.
•	Expose state via getters only.
•	Replace plain setters with guarded mutators that return boolean and do nothing on invalid input.
2) Validation rules (keep it simple)
•	id: not null, trimmed length >= 2.
•	name: not null, trimmed length >= 2.
•	description: can be null or trimmed; if not null, length <= 200.
•	price: >= 0.0 and <= 1_000_000.0.
•	quantity: >= 0 and <= 1_000_000.
•	category: not null.
•	Discounts: percent must be 0..90. If outside → ignore and return original price in preview.
3) Guarded mutators (proposed signatures)
// inside product.Product
public boolean trySetId(String id);
public boolean trySetName(String name);
public boolean trySetDescription(String description);
public boolean trySetPrice(double price);
public boolean trySetQuantity(int quantity);
public boolean trySetCategory(category.Category category);
•	Return true only if the field is updated.
•	On invalid input: return false and do not change the field.
4) Use guards in inventory methods
•	addStock(int amount): accept only amount > 0; update quantity; return true/false.
•	sellProduct(int amount): only if amount > 0 and amount <= quantity; update quantity; return true/false.
•	applyDiscount(double percent): if percent not in 0..90, do nothing and return false; if valid, apply new price and return true.
o	(If your Task 1 had a “preview” method instead of mutation, keep that; just enforce the 0..90 rule.)
•	calculateTotalValue(): unchanged (but now it relies on guarded values).
5) Computed stock status (no direct setter)
•	Remove any public setter for “stock status”.
•	Provide a derived read-only method:
public String getStockStatus(); // returns "OUT_OF_STOCK" (0), "LOW" (1..10), or "IN_STOCK" (>10)
•	Thresholds: 0 → OUT_OF_STOCK, 1..10 → LOW, >10 → IN_STOCK.
6) Category safety (tiny change)
•	In category.Category, make addProduct(Product p) return boolean:
o	Reject null.
o	Reject duplicates of the same object reference (simple contains check).
•	removeProduct(Product p) can stay as in Task 1 (or return boolean if you prefer).
________________________________________
🧪 Demo requirements (product.ShopDemo)
Update your demo to prove guards work:
1.	Create a Category (e.g., “Stationery”) and a Product (e.g., Pen).
2.	Accepted updates:
o	trySetPrice(250.0) → true
o	addStock(20) when quantity was, say, 5 → true → quantity becomes 25
3.	Rejected updates:
o	trySetPrice(-1.0) → false (price unchanged)
o	trySetName("A") → false (name unchanged)
o	sellProduct(10_000) if quantity is lower → false (quantity unchanged)
o	applyDiscount(200) → false (ignored)
4.	Show getStockStatus() before/after restocks/sales.
5.	Add the product to the category twice; the second add must be rejected.
Print each action and the resulting state so it’s obvious what passed/failed.
________________________________________
✅ Acceptance Criteria
•	All Product fields are private; external code cannot mutate them directly.
•	Guarded mutators above exist and preserve old values on invalid input.
•	Inventory methods (addStock, sellProduct, applyDiscount) validate inputs and return boolean.
•	getStockStatus() is computed from quantity (no direct setter).
•	Category.addProduct rejects null and duplicates (by reference).
•	ShopDemo prints at least 2 accepted and 3 rejected actions.
________________________________________
📊 Suggested Grading ( /10 )
•	Encapsulation (private fields + getters) — 2
•	Guarded mutators work (return boolean, no state corruption) — 4
•	Inventory methods validate correctly — 2
•	Demo proves both accepted/rejected flows + stock status — 2

