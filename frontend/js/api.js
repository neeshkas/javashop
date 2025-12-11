/**
 * API Wrapper для работы с REST API музыкального магазина
 * Стиль Цоя и "Кино" - Минимализм и простота
 */

// Для разработки используем mock данные (позже заменить на реальный API)
const USE_MOCK = false;
const API_BASE = 'http://localhost:8086/api';

class ShopAPI {
  /**
   * Mock данные для разработки (пока нет бэкенда)
   */
  static getMockProducts() {
    return [
      {
            "id": "1",
            "name": "????????????? \"Fender Telecaster\"",
            "description": "???????? ????? ? ????? ??????? ? ???????? ??????????.",
            "price": 25000,
            "quantity": 5,
            "category": "guitars",
            "stockStatus": "LOW",
            "type": "physical",
            "image": "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=400&h=300&fit=crop&auto=format&q=80"
      },
      {
            "id": "2",
            "name": "????? \"???????? - ????? ??????\"",
            "description": "????? 1988 ????, ????????????? ?????.",
            "price": 2500,
            "quantity": 100,
            "category": "vinyl",
            "stockStatus": "IN_STOCK",
            "type": "digital",
            "image": "https://images.unsplash.com/photo-1619983081563-430f63602796?w=400&h=300&fit=crop&auto=format&q=80"
      },
      {
            "id": "3",
            "name": "?????????? Yamaha DX7",
            "description": "FM-???? 80-?, ??????? ?????????.",
            "price": 150000,
            "quantity": 2,
            "category": "synths",
            "stockStatus": "LOW",
            "type": "physical",
            "image": "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=400&h=300&fit=crop&auto=format&q=80"
      }
];
  }

  // ========== PRODUCTS ==========

  /**
   * Получить все товары
   */
  static async getProducts() {
    if (USE_MOCK) {
      return new Promise(resolve => {
        setTimeout(() => resolve(this.getMockProducts()), 500);
      });
    }

    try {
      const response = await fetch(`${API_BASE}/products`);
      if (!response.ok) throw new Error('Failed to fetch products');
      return await response.json();
    } catch (error) {
      console.error('Error fetching products:', error);
      throw error;
    }
  }

  /**
   * Получить один товар по ID
   */
  static async getProduct(id) {
    if (USE_MOCK) {
      const products = this.getMockProducts();
      return products.find(p => p.id === id) || null;
    }

    try {
      const response = await fetch(`${API_BASE}/products/${id}`);
      if (!response.ok) throw new Error('Product not found');
      return await response.json();
    } catch (error) {
      console.error('Error fetching product:', error);
      throw error;
    }
  }

  /**
   * Создать товар (для админки)
   */
  static async createProduct(productData) {
    if (USE_MOCK) {
      console.log('Mock: Creating product', productData);
      return { ...productData, id: Date.now().toString() };
    }

    try {
      const response = await fetch(`${API_BASE}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      });
      if (!response.ok) throw new Error('Failed to create product');
      return await response.json();
    } catch (error) {
      console.error('Error creating product:', error);
      throw error;
    }
  }

  /**
   * Обновить товар
   */
  static async updateProduct(id, productData) {
    if (USE_MOCK) {
      console.log('Mock: Updating product', id, productData);
      return { ...productData, id };
    }

    try {
      const response = await fetch(`${API_BASE}/products/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      });
      if (!response.ok) throw new Error('Failed to update product');
      return await response.json();
    } catch (error) {
      console.error('Error updating product:', error);
      throw error;
    }
  }

  /**
   * Удалить товар
   */
  static async deleteProduct(id) {
    if (USE_MOCK) {
      console.log('Mock: Deleting product', id);
      return true;
    }

    try {
      const response = await fetch(`${API_BASE}/products/${id}`, {
        method: 'DELETE'
      });
      if (!response.ok) throw new Error('Failed to delete product');
      return true;
    } catch (error) {
      console.error('Error deleting product:', error);
      throw error;
    }
  }

  // ========== PRICING / PROMOTIONS ==========

  /**
   * Получить все промо-акции
   */
  static async getPromotions() {
    const mockPromotions = [
      { id: 'none', name: 'Без скидки', type: 'none' },
      { id: 'percentage-15', name: '15% на всё', type: 'percentage', value: 15 },
      { id: 'fixed-5000', name: 'Фикс. скидка 5000₽', type: 'fixed', value: 5000 },
      { id: 'bogo-half', name: 'Второй товар -50%', type: 'bogo-half' },
      { id: 'buy3-pay2', name: '3 по цене 2', type: 'buy3-pay2' }
    ];

    if (USE_MOCK) {
      return Promise.resolve(mockPromotions);
    }

    try {
      const response = await fetch(`${API_BASE}/promotions`);
      return await response.json();
    } catch (error) {
      console.error('Error fetching promotions:', error);
      return mockPromotions;
    }
  }

  /**
   * Рассчитать цену с промо
   */
  static calculatePriceWithPromotion(basePrice, quantity, promotion) {
    if (!promotion || promotion.id === 'none') {
      return basePrice * quantity;
    }

    switch (promotion.type) {
      case 'percentage':
        return basePrice * quantity * (1 - promotion.value / 100);

      case 'fixed':
        const discountedUnit = Math.max(0, basePrice - promotion.value);
        return discountedUnit * quantity;

      case 'bogo-half':
        const pairs = Math.floor(quantity / 2);
        const singles = quantity % 2;
        return pairs * (basePrice * 1.5) + singles * basePrice;

      case 'buy3-pay2':
        if (quantity < 3) return basePrice * quantity;
        const freeItems = Math.floor(quantity / 3);
        return basePrice * (quantity - freeItems);

      default:
        return basePrice * quantity;
    }
  }

  // ========== TAX ==========

  /**
   * Получить налоговые политики
   */
  static async getTaxPolicies() {
    const mockTaxes = [
      { id: 'no-tax', name: 'Без налога', rate: 0 },
      { id: 'flat-vat-12', name: 'НДС 12%', rate: 0.12 },
      { id: 'flat-vat-5', name: 'НДС 5% (цифровые товары)', rate: 0.05, digitalOnly: true },
      { id: 'progressive', name: 'Прогрессивный НДС', type: 'progressive' }
    ];

    if (USE_MOCK) {
      return Promise.resolve(mockTaxes);
    }

    try {
      const response = await fetch(`${API_BASE}/taxes`);
      return await response.json();
    } catch (error) {
      console.error('Error fetching tax policies:', error);
      return mockTaxes;
    }
  }

  /**
   * Рассчитать налог
   * Прогрессивная шкала (автоматически):
   * - До 10,000₽ → 5%
   * - До 50,000₽ → 10%
   * - Свыше 50,000₽ → 15%
   */
  static calculateTax(subtotal, taxPolicy, isDigital = false) {
    if (!taxPolicy || taxPolicy.id === 'no-tax') {
      return 0;
    }

    if (taxPolicy.type === 'progressive') {
      if (subtotal <= 10000) return subtotal * 0.05;
      if (subtotal <= 50000) return subtotal * 0.10;
      return subtotal * 0.15;
    }

    if (taxPolicy.digitalOnly && !isDigital) {
      return 0;
    }

    return subtotal * taxPolicy.rate;
  }

  // ========== SHIPPING ==========

  /**
   * Получить политики доставки
   */
  static async getShippingPolicies() {
    const mockShipping = [
      { id: 'none', name: 'Самовывоз (бесплатно)', cost: 0 },
      { id: 'pigeon-standard', name: 'Голубь почтовый (2-3 дня)', cost: 800 },
      { id: 'pigeon-express', name: 'Экспресс-голубь (в тот же день)', cost: 2500 },
      { id: 'pigeon-vip', name: 'VIP голубь с GPS-трекером', cost: 5000 },
      { id: 'leha-delivery', name: 'Лёха принесёт (1-2 часа)', cost: 1500 },
      { id: 'free-over-50k', name: 'Бесплатный голубь при заказе > 50,000₸', cost: 0, threshold: 50000 }
    ];

    if (USE_MOCK) {
      return Promise.resolve(mockShipping);
    }

    try {
      const response = await fetch(`${API_BASE}/shipping-policies`);
      return await response.json();
    } catch (error) {
      console.error('Error fetching shipping policies:', error);
      return mockShipping;
    }
  }

  /**
   * Рассчитать доставку
   */
  static calculateShipping(subtotal, shippingPolicy) {
    if (!shippingPolicy || shippingPolicy.id === 'none') {
      return 0;
    }

    if (shippingPolicy.threshold && subtotal >= shippingPolicy.threshold) {
      return 0;
    }

    return shippingPolicy.cost;
  }

  // ========== CART CHECKOUT ==========

  /**
   * Оформить заказ (расчёт итоговой цены)
   */
  static async checkout(cartItems, promotionId, taxPolicyId, shippingPolicyId) {
    // Получаем политики
    const promotions = await this.getPromotions();
    const taxes = await this.getTaxPolicies();
    const shipping = await this.getShippingPolicies();

    const promotion = promotions.find(p => p.id === promotionId);
    const taxPolicy = taxes.find(t => t.id === taxPolicyId);
    const shippingPolicy = shipping.find(s => s.id === shippingPolicyId);

    // Расчёт
    let itemsTotal = 0;
    let itemsTotalWithPromo = 0;
    const itemBreakdown = [];

    cartItems.forEach(item => {
      const basePrice = item.product.price * item.quantity;
      const priceWithPromo = this.calculatePriceWithPromotion(
        item.product.price,
        item.quantity,
        promotion
      );

      itemsTotal += basePrice;
      itemsTotalWithPromo += priceWithPromo;

      itemBreakdown.push({
        productName: item.product.name,
        quantity: item.quantity,
        basePrice,
        priceWithPromo,
        discount: basePrice - priceWithPromo
      });
    });

    const subtotal = itemsTotalWithPromo;
    const discountAmount = itemsTotal - itemsTotalWithPromo;

    // Налог считается от subtotal (без доставки)
    const hasDigitalProducts = cartItems.some(item => item.product.type === 'digital');
    const taxAmount = this.calculateTax(subtotal, taxPolicy, hasDigitalProducts);

    // Доставка
    const shippingCost = this.calculateShipping(subtotal, shippingPolicy);

    const total = subtotal + taxAmount + shippingCost;

    return {
      itemBreakdown,
      itemsTotal,
      subtotal,
      discountAmount,
      promotion: promotion ? promotion.name : 'Нет',
      taxAmount,
      taxPolicy: taxPolicy ? taxPolicy.name : 'Нет',
      shippingCost,
      shippingPolicy: shippingPolicy ? shippingPolicy.name : 'Нет',
      total
    };
  }
}
