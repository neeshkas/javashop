/**
 * Cart JS - Логика корзины
 * Стиль "Кино" - Side A / Side B (как на пластинке)
 */

let cart = [];
let promotions = [];
let taxes = [];
let shippingPolicies = [];

let selectedPromotion = null;
let selectedTax = null;  // Налог теперь всегда progressive (автоматически)
let selectedShipping = null;

// ========== INIT ==========

document.addEventListener('DOMContentLoaded', async () => {
  // Загрузить корзину из localStorage
  loadCart();

  // Загрузить политики
  await loadPolicies();

  // Рендер
  renderCart();
  renderPolicySelectors();

  // Инициализировать обработчики
  initEventListeners();

  // Рассчитать цены
  await calculateTotal();
});

// ========== LOAD CART ==========

function loadCart() {
  cart = JSON.parse(localStorage.getItem('cart') || '[]');
}

function saveCart() {
  localStorage.setItem('cart', JSON.stringify(cart));
}

// ========== LOAD POLICIES ==========

async function loadPolicies() {
  try {
    promotions = await ShopAPI.getPromotions();
    taxes = await ShopAPI.getTaxPolicies();
    shippingPolicies = await ShopAPI.getShippingPolicies();

    // Выбрать дефолтные
    selectedPromotion = promotions[0];
    // Налог всегда progressive (автоматически)
    selectedTax = taxes.find(t => t.id === 'progressive') || taxes[0];
    selectedShipping = shippingPolicies[0];
  } catch (error) {
    console.error('Error loading policies:', error);
  }
}

// ========== RENDER CART ==========

function renderCart() {
  const emptyCart = document.getElementById('empty-cart');
  const cartContent = document.getElementById('cart-content');
  const cartItemsEl = document.getElementById('cart-items');

  if (cart.length === 0) {
    emptyCart.classList.remove('hidden');
    cartContent.classList.add('hidden');
    return;
  }

  emptyCart.classList.add('hidden');
  cartContent.classList.remove('hidden');

  cartItemsEl.innerHTML = cart.map((item, index) => `
    <div style="
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px;
      border-bottom: 1px solid var(--concrete);
      flex-wrap: wrap;
      gap: 15px;
    ">
      <div style="flex: 1; min-width: 200px;">
        <h4 style="color: var(--white); margin-bottom: 5px; font-size: 1.1rem;">
          ${index + 1}. ${item.product.name}
        </h4>
        <p style="color: var(--light-grey); font-size: 0.9rem;">
          ₽${item.product.price.toLocaleString()} × ${item.quantity}
        </p>
      </div>

      <div style="display: flex; align-items: center; gap: 15px;">
        <!-- Quantity Controls -->
        <div style="display: flex; align-items: center; gap: 10px;">
          <button
            onclick="updateQuantity('${item.productId}', ${item.quantity - 1})"
            style="
              background: var(--charcoal);
              border: 1px solid var(--white);
              color: var(--white);
              width: 30px;
              height: 30px;
              cursor: pointer;
              font-size: 1.2rem;
            "
          >-</button>

          <span style="color: var(--white); font-size: 1.1rem; min-width: 30px; text-align: center;">
            ${item.quantity}
          </span>

          <button
            onclick="updateQuantity('${item.productId}', ${item.quantity + 1})"
            style="
              background: var(--charcoal);
              border: 1px solid var(--white);
              color: var(--white);
              width: 30px;
              height: 30px;
              cursor: pointer;
              font-size: 1.2rem;
            "
          >+</button>
        </div>

        <!-- Total -->
        <div style="min-width: 120px; text-align: right; color: var(--soviet-red); font-size: 1.2rem; font-weight: bold;">
          ₽${(item.product.price * item.quantity).toLocaleString()}
        </div>

        <!-- Remove -->
        <button
          onclick="removeFromCart('${item.productId}')"
          style="
            background: transparent;
            border: 2px solid var(--soviet-red);
            color: var(--soviet-red);
            padding: 8px 15px;
            cursor: pointer;
            text-transform: uppercase;
            font-family: var(--font-heading);
            letter-spacing: 1px;
            font-size: 0.85rem;
          "
        >Удалить</button>
      </div>
    </div>
  `).join('');
}

// ========== RENDER POLICY SELECTORS ==========

function renderPolicySelectors() {
  // Promotions
  const promotionSelect = document.getElementById('promotion-select');
  promotionSelect.innerHTML = promotions.map(promo => `
    <option value="${promo.id}">${promo.name}</option>
  `).join('');
  promotionSelect.value = selectedPromotion.id;

  // Tax - скрыт, т.к. автоматический
  // (selectedTax уже установлен в loadPolicies)

  // Shipping (голуби!)
  const shippingSelect = document.getElementById('shipping-select');
  shippingSelect.innerHTML = shippingPolicies.map(shipping => `
    <option value="${shipping.id}">
      ${shipping.id.includes('pigeon') || shipping.id === 'express' ? '🕊️ ' : ''}${shipping.name}
    </option>
  `).join('');
  shippingSelect.value = selectedShipping.id;
}

// ========== CART ACTIONS ==========

function updateQuantity(productId, newQuantity) {
  if (newQuantity <= 0) {
    removeFromCart(productId);
    return;
  }

  const item = cart.find(i => i.productId === productId);
  if (item) {
    item.quantity = newQuantity;
    saveCart();
    renderCart();
    calculateTotal();
  }
}

function removeFromCart(productId) {
  cart = cart.filter(item => item.productId !== productId);
  saveCart();
  renderCart();
  calculateTotal();
}

function clearCart() {
  cart = [];
  saveCart();
  renderCart();
  calculateTotal();
}

// ========== CALCULATE TOTAL ==========

async function calculateTotal() {
  if (cart.length === 0) {
    return;
  }

  try {
    const checkout = await ShopAPI.checkout(
      cart,
      selectedPromotion.id,
      selectedTax.id,
      selectedShipping.id
    );

    // Update UI
    document.getElementById('items-total').textContent =
      `₽${checkout.itemsTotal.toLocaleString()}`;

    document.getElementById('discount-amount').textContent =
      `-₽${checkout.discountAmount.toLocaleString()}`;

    document.getElementById('tax-amount').textContent =
      `+₽${checkout.taxAmount.toLocaleString()}`;

    document.getElementById('shipping-cost').textContent =
      `+₽${checkout.shippingCost.toLocaleString()}`;

    document.getElementById('total-amount').textContent =
      `₽${checkout.total.toLocaleString()}`;

  } catch (error) {
    console.error('Error calculating total:', error);
  }
}

// ========== EVENT LISTENERS ==========

function initEventListeners() {
  // Promotion change
  document.getElementById('promotion-select').addEventListener('change', (e) => {
    selectedPromotion = promotions.find(p => p.id === e.target.value);
    calculateTotal();
  });

  // Tax - НЕТ обработчика, т.к. автоматический расчёт

  // Shipping change (голуби!)
  document.getElementById('shipping-select').addEventListener('change', (e) => {
    selectedShipping = shippingPolicies.find(s => s.id === e.target.value);
    calculateTotal();
  });

  // Checkout button
  document.getElementById('checkout-btn').addEventListener('click', () => {
    if (cart.length === 0) {
      alert('Корзина пуста!');
      return;
    }

    // В реальном приложении здесь был бы API вызов
    alert(`
      Заказ оформлен! 🎸

      Спасибо за покупку в магазине "Перемен"!

      (В реальном приложении здесь был бы переход на страницу оплаты)

      Цой жив! ❤️
    `);

    // Очистить корзину
    clearCart();

    // Перенаправить на главную
    setTimeout(() => {
      window.location.href = 'index.html';
    }, 1500);
  });
}
