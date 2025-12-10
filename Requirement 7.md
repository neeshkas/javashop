Abstract Classes & Interfaces: Promotions (Template Method) + Tax & Shipping Interfaces
Builds on: Task 1–5 (Product, PhysicalProduct, DigitalProduct, price rules).
Scope: Keep the domain minimal (Products only). We will add one abstract class for promotions and two small interfaces for cross-cutting behavior.

1.	Refactor pricing rules to showcase an abstract class with a Template Method.
2.	Add interfaces for tax and shipping that different product types can implement/use.

You will:
•	Create abstract class Promotion that implements PricePolicy and factors shared logic.
•	Implement three concrete promotions by extending Promotion.
•	Add interfaces TaxPolicy and Shippable and wire them into price calculation.
•	Demonstrate polymorphism with lists of Promotion and different TaxPolicy on PhysicalProduct vs DigitalProduct.


📁 Project Structure
src/
└── product/
    ├── Product.java
    ├── PhysicalProduct.java
    ├── DigitalProduct.java
    ├── pricing/
    │   ├── PricePolicy.java            # from Task 5 (keep it)
    │   ├── Promotion.java              # NEW (abstract class)
    │   ├── PercentagePromotion.java    # extends Promotion
    │   ├── FixedPromotion.java         # extends Promotion
    │   └── BogoHalfPromotion.java      # extends Promotion (pair logic)
    ├── tax/
    │   ├── TaxPolicy.java              # NEW (interface)
    │   ├── NoTax.java                  # implements TaxPolicy
    │   ├── FlatVat.java                # implements TaxPolicy (e.g., 12%)
    │   └── ReducedDigitalVat.java      # implements TaxPolicy (e.g., 5% only for DigitalProduct)
    ├── shipping/
    │   └── Shippable.java              # NEW (interface)
    └── ShopDemo6.java

> прочитай requirement 7 и объясни что в ней нового (абсолютно все изменения, новые классы, методы, логику) по
сравнению с остальным проектом

● Я помогу вам разобраться с requirement 7. Сначала найду этот файл и прочитаю его, а затем сравню с остальным
проектом.

● Search(pattern: "**/*requirement*")
⎿  Found 0 files

● Search(pattern: "**/*req*")
⎿  Found 0 files

● Search(pattern: "**/*.md")
⎿  Found 7 files (ctrl+o to expand)

● Search(pattern: "**/*.txt")
⎿  Found 3 files (ctrl+o to expand)

● Read(Requirement 7.md)
⎿  Read 36 lines

● Read(Requirement 6.md)
⎿  Read 197 lines

● Search(pattern: "**/*.java")
⎿  Found 21 files (ctrl+o to expand)

● Read(src\product\pricing\Promotion.java)
⎿  Read 49 lines

● Read(src\product\pricing\PercentagePromotion.java)
⎿  Read 25 lines

● Read(src\product\tax\TaxPolicy.java)
⎿  Read 32 lines

● Read(src\product\tax\FlatVat.java)
⎿  Read 31 lines

● Read(src\product\shipping\Shippable.java)
⎿  Read 22 lines

● Read(src\product\ShopDemo6.java)
⎿  Read 227 lines

● Read(src\product\pricing\FixedPromotion.java)
⎿  Read 25 lines

● Read(src\product\pricing\BogoHalfPromotion.java)
⎿  Read 23 lines

● Read(src\product\tax\NoTax.java)
⎿  Read 25 lines

● Read(src\product\tax\ReducedDigitalVat.java)
⎿  Read 35 lines

● Read(src\product\pricing\PercentageOff.java)
⎿  Read 23 lines

● Read(src\product\PhysicalProduct.java)
⎿  Read 134 lines

● Отлично! Я изучил Requirement 7 и весь код. Вот полное объяснение всех изменений по сравнению с предыдущими
требованиями:

  ---
📦 Requirement 7: Абстрактные классы и интерфейсы

🔴 ГЛАВНАЯ ИДЕЯ

Requirement 7 рефакторит существующую систему скидок и добавляет две новые функциональности: налогообложение и
доставку через интерфейсы. Также демонстрирует паттерн Template Method.

  ---
1️⃣ НОВЫЙ АБСТРАКТНЫЙ КЛАСС: Promotion (Template Method Pattern)

ЧТО БЫЛО (Requirement 6):

// Классы НАПРЯМУЮ реализовывали интерфейс PricePolicy
public class PercentageOff implements PricePolicy {
@Override
public double apply(Product p, int qty) {
// Вся логика валидации + расчет скидки
double unit = p.getPrice() * (1 - percent / 100.0);
return unit * Math.max(0, qty);
}
}

ЧТО СТАЛО (Requirement 7):

// Абстрактный класс с Template Method
public abstract class Promotion implements PricePolicy {
// FINAL метод - общий алгоритм (Template)
@Override
public final double apply(Product p, int qty) {
if (qty <= 0) return 0.0;
double basePrice = p.getPrice();
double totalPrice = calculateDiscount(basePrice, qty);
return Math.max(0.0, totalPrice);
}

      // ABSTRACT метод - хук для подклассов
      protected abstract double calculateDiscount(double basePrice, int qty);
}

НОВЫЕ КЛАССЫ-НАСЛЕДНИКИ:

- PercentagePromotion extends Promotion
- FixedPromotion extends Promotion
- BogoHalfPromotion extends Promotion

Логика: Теперь они только реализуют calculateDiscount(), а валидация qty и защита от отрицательных значений
делается в родителе Promotion.

  ---
2️⃣ НОВЫЙ ПАКЕТ: tax/ (Налогообложение)

Интерфейс TaxPolicy:

public interface TaxPolicy {
String name();
double calculateTax(Product p, double subtotal);
default boolean applicableTo(Product p) { return true; }
}

Три реализации:

NoTax - нулевой налог

public class NoTax implements TaxPolicy {
public double calculateTax(Product p, double subtotal) {
return 0.0;
}
}

FlatVat - фиксированный НДС для всех товаров

public class FlatVat implements TaxPolicy {
private final double vatRate; // например, 0.12 = 12%

      public double calculateTax(Product p, double subtotal) {
          return subtotal * vatRate;
      }
}

ReducedDigitalVat - пониженный НДС ТОЛЬКО для цифровых товаров

public class ReducedDigitalVat implements TaxPolicy {
private final double vatRate; // например, 0.05 = 5%

      public double calculateTax(Product p, double subtotal) {
          if (applicableTo(p)) {
              return subtotal * vatRate;
          }
          return 0.0;
      }

      @Override
      public boolean applicableTo(Product p) {
          return p instanceof DigitalProduct; // 🔥 Типоспецифичное поведение
      }
}

КЛЮЧЕВОЙ МОМЕНТ: ReducedDigitalVat использует instanceof для проверки типа продукта!

  ---
3️⃣ НОВЫЙ ПАКЕТ: shipping/ (Доставка)

Интерфейс Shippable:

public interface Shippable {
double getShippingCost();
default boolean requiresShipping() { return true; }
}

PhysicalProduct теперь implements Shippable:

public class PhysicalProduct extends Product implements Shippable {
// Ранее был просто метод estimateShippingCost()

      @Override
      public double getShippingCost() {
          return estimateShippingCost(); // делегирует старому методу
      }

      @Override
      public boolean requiresShipping() {
          return true; // физические товары всегда требуют доставки
      }
}

ЧТО ИЗМЕНИЛОСЬ: Раньше estimateShippingCost() был просто методом класса. Теперь это часть интерфейса Shippable,
что позволяет полиморфно работать с доставкой.

  ---
4️⃣ НОВАЯ ЛОГИКА ЦЕНООБРАЗОВАНИЯ

Порядок расчета финальной цены:

1. Базовая цена (qty × price)
2. Применение промоушена (выбор лучшего из списка)
3. Вычет доставки из subtotal (для расчета налога)
4. Расчет налога (на subtotal БЕЗ доставки)
5. Добавление доставки обратно
6. ИТОГО = subtotal + tax + shipping

Пример из ShopDemo6:

double subtotal = bestPrice; // цена после промоушена
if (product instanceof Shippable) {
shipping = ((Shippable) product).getShippingCost();
subtotal -= shipping; // вычитаем для расчета налога
}

double taxAmount = tax.calculateTax(product, subtotal);
double finalTotal = subtotal + taxAmount + shipping;

  ---
5️⃣ ДЕМОНСТРАЦИЯ ПОЛИМОРФИЗМА В ShopDemo6

Полиморфизм через абстрактный класс:

List<Promotion> promotions = List.of(
new PercentagePromotion(15),
new FixedPromotion(10000),
new BogoHalfPromotion()
);

for (Promotion promo : promotions) { // 🔥 Все имеют общий тип Promotion
double price = promo.apply(product, 1);
}

Полиморфизм через интерфейс:

TaxPolicy noTax = new NoTax();
TaxPolicy flatVat = new FlatVat(0.12);
TaxPolicy reducedVat = new ReducedDigitalVat(0.05);

// Все реализуют TaxPolicy
double tax = reducedVat.calculateTax(ebook, 10000); // 500 KZT (5%)
double tax2 = reducedVat.calculateTax(laptop, 10000); // 0 KZT (не применимо)

Типоспецифичное поведение:

if (product instanceof Shippable) {
Shippable shippable = (Shippable) product;
double cost = shippable.getShippingCost();
}

  ---
📊 СРАВНИТЕЛЬНАЯ ТАБЛИЦА

| Аспект                 | До (Requirement 6)                    | После (Requirement 7)                |
  |------------------------|---------------------------------------|--------------------------------------|
| Промоушены             | Напрямую implements PricePolicy       | extends Promotion (Template Method)  |
| Валидация qty          | В каждом классе отдельно              | Один раз в Promotion.apply()         |
| Налоги                 | ❌ Отсутствуют                         | ✅ Интерфейс TaxPolicy + 3 реализации |
| Доставка               | estimateShippingCost() (просто метод) | Интерфейс Shippable                  |
| Типоспецифичные налоги | ❌ Нет                                 | ✅ ReducedDigitalVat (instanceof)     |
| Финальная цена         | base + shipping                       | base + best_promo + tax + shipping   |

  ---
🎯 КЛЮЧЕВЫЕ ПАТТЕРНЫ

1. Template Method Pattern (Promotion):
   - apply() — финальный шаблон
   - calculateDiscount() — абстрактный хук
2. Strategy Pattern (TaxPolicy, Shippable):
   - Разные стратегии налогообложения и доставки
3. Полиморфизм:
   - Через абстрактный класс (Promotion)
   - Через интерфейсы (TaxPolicy, Shippable)
4. Type-based behavior (instanceof):
   - ReducedDigitalVat применяется только к DigitalProduct

  ---
Всё! Requirement 7 добавляет систему налогообложения, формализует доставку через интерфейс и рефакторит промоушены
с использованием Template Method.

> что такое subtotal

● Subtotal (подытог) — это промежуточная сумма после применения скидок, но до добавления налогов и доставки.

  ---
📊 ПОРЯДОК РАСЧЕТА ЦЕНЫ

1. Base Price (базовая цена)       = qty × price
2. Promotion (скидка)               = применяется к base price
3. ➡️ SUBTOTAL                      = base price - discount
4. Tax (налог)                      = subtotal × tax_rate
5. Shipping (доставка)              = фиксированная сумма
6. FINAL TOTAL (итого)              = subtotal + tax + shipping

  ---
💡 ПРИМЕР ИЗ РЕАЛЬНОЙ ЖИЗНИ

Представь чек из магазина:

Ноутбук                     150,000 KZT
Скидка -15%                 -22,500 KZT
─────────────────────────────────────────
ПОДЫТОГ (Subtotal):         127,500 KZT  ⬅️ ЭТО SUBTOTAL
НДС 12%:                     15,300 KZT
Доставка:                     2,500 KZT
─────────────────────────────────────────
ИТОГО (Final Total):        145,300 KZT

ВАЖНО: Налог считается от subtotal (127,500), а НЕ от базовой цены (150,000)!

  ---
🔍 КАК ЭТО РАБОТАЕТ В КОДЕ

Из ShopDemo6.java:96-147

// 1. Базовая цена БЕЗ промоушена
double baseTotal = product.finalPrice(qty);

// 2. Применяем лучший промоушен
double bestPrice = product.finalPrice(qty, bestPromo);

// 3. Получаем SUBTOTAL (цена после скидки, БЕЗ доставки)
double subtotal = bestPrice;
double shipping = 0.0;

// Если физический товар - вычитаем доставку из subtotal
if (product instanceof Shippable) {
shipping = ((Shippable) product).getShippingCost();
subtotal -= shipping; // ⬅️ ВАЖНО! subtotal БЕЗ доставки
}

// 4. Налог считается от SUBTOTAL (БЕЗ доставки!)
double taxAmount = tax.calculateTax(product, subtotal);

// 5. Финальная цена = subtotal + налог + доставка
double finalTotal = subtotal + taxAmount + shipping;

  ---
❓ ПОЧЕМУ ВЫЧИТАЕТСЯ ДОСТАВКА?

Потому что product.finalPrice(qty, promo) для PhysicalProduct уже включает доставку:

// PhysicalProduct.java:103-106
@Override
public double finalPrice(int qty, PricePolicy policy) {
double base = super.finalPrice(qty, policy); // скидка применена
return base + estimateShippingCost(); // ⬅️ доставка УЖЕ добавлена
}

Поэтому в ShopDemo6:
1. bestPrice = цена после промоушена + доставка
2. subtotal = bestPrice - shipping = цена после промоушена БЕЗ доставки
3. Налог считается от subtotal (без доставки)
4. Потом доставка добавляется обратно

  ---
📝 КОНКРЕТНЫЙ ПРИМЕР

PhysicalProduct laptop = new PhysicalProduct("LP001", "Laptop", 150000.0, 2.5);
// shipping = 250 KZT

int qty = 1;
Promotion promo = new PercentagePromotion(15); // -15%
TaxPolicy tax = new FlatVat(0.12); // 12%

// Расчет:
baseTotal = 150000 + 250 = 150250 KZT

bestPrice = 150000 × 0.85 + 250 = 127750 KZT
(цена после скидки + доставка)

subtotal = 127750 - 250 = 127500 KZT  ⬅️ SUBTOTAL
(цена после скидки БЕЗ доставки)

taxAmount = 127500 × 0.12 = 15300 KZT
(налог от SUBTOTAL)

finalTotal = 127500 + 15300 + 250 = 143050 KZT
(subtotal + налог + доставка)

  ---
🎯 ИТОГО

Subtotal = Сумма после скидок, но до налогов и доставки.

Это промежуточное значение для правильного расчета налогов (налог не должен начисляться на доставку в данной
модели).
