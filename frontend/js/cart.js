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

  // Обновить счётчик корзины
  updateCartCount();

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

    // Налог всегда progressive (автоматически)
    selectedTax = taxes.find(t => t.id === 'progressive') || taxes[0];
    selectedShipping = shippingPolicies[0];

    // Промо будет выбираться автоматически в calculateTotal()
    selectedPromotion = promotions[0]; // дефолт на случай пустой корзины
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
          ₸${item.product.price.toLocaleString()} × ${item.quantity}
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
          ₸${(item.product.price * item.quantity).toLocaleString()}
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
  // Promotions - автоматический выбор, селектор удалён

  // Tax - автоматический расчёт, селектор удалён

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
    updateCartCount();
  }
}

function removeFromCart(productId) {
  cart = cart.filter(item => item.productId !== productId);
  saveCart();
  renderCart();
  calculateTotal();
  updateCartCount();
}

function clearCart() {
  cart = [];
  saveCart();
  renderCart();
  calculateTotal();
  updateCartCount();
}

function updateCartCount() {
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  const cartCountEl = document.getElementById('cart-count');
  if (cartCountEl) {
    cartCountEl.textContent = totalItems;
  }
}

// ========== CALCULATE TOTAL ==========

async function calculateTotal() {
  if (cart.length === 0) {
    return;
  }

  try {
    // Автоматически найти лучшую промо (которая даёт максимальную скидку)
    let bestPromotion = promotions[0]; // дефолт "Без скидки"
    let maxDiscount = 0;

    // Перебрать все промо и выбрать ту, что даёт наибольшую экономию
    for (const promo of promotions) {
      const testCheckout = await ShopAPI.checkout(
        cart,
        promo.id,
        selectedTax.id,
        selectedShipping.id
      );
      if (testCheckout.discountAmount > maxDiscount) {
        maxDiscount = testCheckout.discountAmount;
        bestPromotion = promo;
      }
    }

    selectedPromotion = bestPromotion;

    // Финальный расчёт с лучшей промо
    const checkout = await ShopAPI.checkout(
      cart,
      selectedPromotion.id,
      selectedTax.id,
      selectedShipping.id
    );

    // Обновить инфо о промо
    const promoInfo = document.getElementById('promo-info');
    if (selectedPromotion.id === 'none') {
      promoInfo.textContent = 'Скидок нет (но мы искали!)';
    } else {
      promoInfo.textContent = `Применена: ${selectedPromotion.name} (экономия ${checkout.discountAmount.toLocaleString()}₸)`;
    }

    // Update UI
    document.getElementById('items-total').textContent =
      `₸${checkout.itemsTotal.toLocaleString()}`;

    document.getElementById('discount-amount').textContent =
      `-₸${checkout.discountAmount.toLocaleString()}`;

    document.getElementById('tax-amount').textContent =
      `+₸${checkout.taxAmount.toLocaleString()}`;

    document.getElementById('shipping-cost').textContent =
      `+₸${checkout.shippingCost.toLocaleString()}`;

    document.getElementById('total-amount').textContent =
      `₸${checkout.total.toLocaleString()}`;

  } catch (error) {
    console.error('Error calculating total:', error);
  }
}

function showPigeonTracking() {
    const modal = document.getElementById('pigeon-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalSubtitle = document.getElementById('modal-subtitle');
    const status = document.getElementById('pigeon-status');
    const closeBtn = document.getElementById('close-pigeon-modal');
    const mapContainer = document.getElementById('pigeon-map');

    // Определить какая доставка выбрана
    const isLehaDelivery = selectedShipping && selectedShipping.id === 'leha-delivery';
    const deliveryName = isLehaDelivery ? 'Лёха' : 'Голубь';

    // Обновить заголовок и текст модалки
    if (isLehaDelivery) {
        modalTitle.innerHTML = '🚀 Лёха в пути! 🚀';
        modalSubtitle.textContent = 'Лёха уже спешит к вам с вашим заказом!';
    } else {
        modalTitle.innerHTML = '🕊️ Голубь в пути! 🕊️';
        modalSubtitle.textContent = 'Ваш заказ доставляется специально обученным почтовым голубем.';
    }

    modal.classList.remove('hidden');

    // Очистить контейнер
    mapContainer.innerHTML = '';

    // Создать карту Алматы с зумом
    const routeDiv = document.createElement('div');
    routeDiv.style.cssText = `
        position: relative;
        width: 100%;
        height: 100%;
        background-image: url('https://api.mapbox.com/styles/v1/mapbox/dark-v11/static/76.8897,43.2389,12,0/900x400@2x?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw');
        background-size: cover;
        background-position: center;
        overflow: hidden;
        cursor: grab;
    `;

    // Добавить контролы зума
    const zoomControls = document.createElement('div');
    zoomControls.style.cssText = `
        position: absolute;
        top: 10px;
        right: 10px;
        display: flex;
        flex-direction: column;
        gap: 5px;
        z-index: 100;
    `;

    const zoomInBtn = document.createElement('button');
    zoomInBtn.textContent = '+';
    zoomInBtn.style.cssText = `
        width: 40px;
        height: 40px;
        background: var(--charcoal);
        border: 2px solid var(--street-yellow);
        color: var(--street-yellow);
        font-size: 24px;
        cursor: pointer;
        font-weight: bold;
    `;

    const zoomOutBtn = document.createElement('button');
    zoomOutBtn.textContent = '−';
    zoomOutBtn.style.cssText = `
        width: 40px;
        height: 40px;
        background: var(--charcoal);
        border: 2px solid var(--street-yellow);
        color: var(--street-yellow);
        font-size: 24px;
        cursor: pointer;
        font-weight: bold;
    `;

    let zoomLevel = 1;

    zoomInBtn.onclick = () => {
        zoomLevel = Math.min(zoomLevel + 0.2, 2);
        routeDiv.style.backgroundSize = `${100 * zoomLevel}%`;
    };

    zoomOutBtn.onclick = () => {
        zoomLevel = Math.max(zoomLevel - 0.2, 0.5);
        routeDiv.style.backgroundSize = `${100 * zoomLevel}%`;
    };

    zoomControls.appendChild(zoomInBtn);
    zoomControls.appendChild(zoomOutBtn);
    routeDiv.appendChild(zoomControls);

    // Линия маршрута
    const routeLine = document.createElement('div');
    routeLine.style.cssText = `
        position: absolute;
        top: 50%;
        left: 10%;
        right: 10%;
        height: 4px;
        background: repeating-linear-gradient(
            90deg,
            #C41E3A,
            #C41E3A 10px,
            transparent 10px,
            transparent 20px
        );
        transform: translateY(-50%);
    `;
    routeDiv.appendChild(routeLine);

    // Магазин (старт)
    const shopMarker = document.createElement('div');
    shopMarker.style.cssText = `
        position: absolute;
        top: 50%;
        left: 10%;
        transform: translate(-50%, -50%);
        width: 50px;
        height: 50px;
        background: #FFD700;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        border: 4px solid #C41E3A;
        box-shadow: 0 0 20px rgba(255, 215, 0, 0.6);
        z-index: 10;
    `;
    shopMarker.textContent = '🏪';
    routeDiv.appendChild(shopMarker);

    // Дом (финиш)
    const homeMarker = document.createElement('div');
    homeMarker.style.cssText = `
        position: absolute;
        top: 50%;
        right: 10%;
        transform: translate(50%, -50%);
        width: 50px;
        height: 50px;
        background: #FFD700;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        border: 4px solid #C41E3A;
        box-shadow: 0 0 20px rgba(255, 215, 0, 0.6);
        z-index: 10;
    `;
    homeMarker.textContent = '🏠';
    routeDiv.appendChild(homeMarker);

    // Курьер (Лёха или Голубь)
    const deliveryMarker = document.createElement('div');
    deliveryMarker.style.cssText = `
        position: absolute;
        top: 50%;
        left: 10%;
        transform: translate(-50%, -50%);
        z-index: 20;
        transition: left 0.1s linear;
    `;

    if (isLehaDelivery) {
        // Фото Лёхи (маленькое)
        deliveryMarker.innerHTML = `
            <div style="
                width: 45px;
                height: 45px;
                border-radius: 50%;
                overflow: hidden;
                border: 3px solid #FFD700;
                background: white;
                box-shadow: 0 0 20px rgba(255, 215, 0, 1), 0 0 40px rgba(255, 215, 0, 0.5);
                animation: pulse 1.5s ease-in-out infinite;
            ">
                <img src="assets/images/Леха.jpg" style="width: 100%; height: 100%; object-fit: cover;">
            </div>
        `;
    } else {
        // Голубь
        deliveryMarker.innerHTML = `
            <div style="
                font-size: 40px;
                text-shadow: 0 0 15px rgba(255, 255, 255, 1);
                animation: fly 1s ease-in-out infinite;
            ">🕊️</div>
        `;
    }

    routeDiv.appendChild(deliveryMarker);
    mapContainer.appendChild(routeDiv);

    // Анимация движения
    let progress = 0;
    status.textContent = `Статус: ${deliveryName} выехал из магазина...`;

    function animate() {
        progress += 0.4; // Медленнее для плавности

        if (progress >= 100) {
            progress = 100;
            status.textContent = `Статус: Доставлено! Цой жив! ❤️`;
            return;
        }

        // Обновить позицию (от 10% до 90%)
        const currentLeft = 10 + (80 * progress / 100);
        deliveryMarker.style.left = `${currentLeft}%`;

        // Обновить статус
        if (progress > 25 && progress < 27) {
            status.textContent = `Статус: ${deliveryName} проезжает мимо парка...`;
        } else if (progress > 50 && progress < 52) {
            status.textContent = `Статус: ${deliveryName} на полпути!`;
        } else if (progress > 75 && progress < 77) {
            status.textContent = `Статус: ${deliveryName} почти у цели!`;
        }

        setTimeout(animate, 100);
    }

    animate();

    // Обработчик закрытия
    closeBtn.onclick = () => {
        modal.classList.add('hidden');
        window.location.href = 'index.html';
    };
}

// ========== EVENT LISTENERS ==========

function initEventListeners() {
  // Promotion - НЕТ обработчика, выбирается автоматически

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

    // Показать трекинг
    showPigeonTracking();

    // Очистить корзину
    clearCart();
    
    // Обновить счётчик в header (если он есть на странице)
    const cartCountEl = document.getElementById('cart-count');
    if (cartCountEl) {
        cartCountEl.textContent = '(0)';
    }
  });
}

