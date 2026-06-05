/**
 * API wrapper для музыкального магазина.
 * Бэкенд является источником товаров, справочников и расчёта checkout.
 */

const API_BASE = window.location.protocol === 'file:'
  ? 'http://localhost:8086/api'
  : `${window.location.origin}/api`;

class ShopAPI {
  static async request(path, options = {}) {
    const headers = {
      Accept: 'application/json',
      ...(options.headers || {})
    };

    if (options.body && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers
    });

    const text = await response.text();
    let data = null;

    if (text) {
      try {
        data = JSON.parse(text);
      } catch (error) {
        throw new Error(`API вернул некорректный JSON: ${text.substring(0, 120)}`);
      }
    }

    if (!response.ok) {
      throw new Error(data?.error || `API request failed: ${response.status}`);
    }

    return data;
  }

  // ========== PRODUCTS ==========

  static async getProducts() {
    return this.request('/products');
  }

  static async getProduct(id) {
    return this.request(`/products/${encodeURIComponent(id)}`);
  }

  static async createProduct(productData) {
    return this.request('/products', {
      method: 'POST',
      body: JSON.stringify(productData)
    });
  }

  static async updateProduct(id, productData) {
    return this.request(`/products/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(productData)
    });
  }

  static async deleteProduct(id) {
    return this.request(`/products/${encodeURIComponent(id)}`, {
      method: 'DELETE'
    });
  }

  // ========== POLICIES ==========

  static async getPromotions() {
    return this.request('/promotions');
  }

  static async getTaxPolicies() {
    return this.request('/taxes');
  }

  static async getShippingPolicies() {
    return this.request('/shipping-policies');
  }

  // ========== CHECKOUT ==========

  static async checkout(cartItems, promotionId = 'auto', taxPolicyId = 'progressive', shippingPolicyId = 'none') {
    const items = cartItems.map(item => ({
      productId: item.productId,
      quantity: item.quantity
    }));

    return this.request('/checkout', {
      method: 'POST',
      body: JSON.stringify({
        items,
        promotionId,
        taxPolicyId,
        shippingPolicyId
      })
    });
  }
}
