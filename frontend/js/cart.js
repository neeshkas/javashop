/**
 * Cart JS - simple select-based shipping
 */

let cart = [];
let promotions = [];
let taxes = [];
let shippingPolicies = [];

let selectedPromotion = null;
let selectedTax = null;      // always progressive by default
let selectedShipping = null; // from dropdown

// ========== INIT ==========

document.addEventListener('DOMContentLoaded', async () => {
  try {
    loadCart();
    await Promise.all([loadPolicies(), syncCartProducts()]);
    updateCartCount();
    renderCart();
    renderPolicySelectors();
    initEventListeners();
    await calculateTotal();
  } catch (error) {
    console.error('Cart initialization failed:', error);
    alert('Не удалось подключиться к серверу магазина.');
  }
});

// ========== LOAD CART ==========

function loadCart() {
  try {
    cart = JSON.parse(localStorage.getItem('cart') || '[]');
  } catch (error) {
    cart = [];
    saveCart();
  }
}

function saveCart() {
  localStorage.setItem('cart', JSON.stringify(cart));
}

// ========== LOAD POLICIES ==========

async function loadPolicies() {
  promotions = await ShopAPI.getPromotions();
  taxes = await ShopAPI.getTaxPolicies();
  shippingPolicies = await ShopAPI.getShippingPolicies();

  selectedTax = taxes.find(t => t.id === 'progressive') || taxes[0] || null;
  selectedShipping = shippingPolicies.find(s => s.id === 'none') || shippingPolicies[0] || null;
  selectedPromotion = promotions.find(p => p.id === 'none') || promotions[0] || null;
}

async function syncCartProducts() {
  if (cart.length === 0) {
    return;
  }

  const products = await ShopAPI.getProducts();
  let changed = false;

  cart = cart.map(item => {
    const product = products.find(p => p.id === item.productId);
    if (!product || product.quantity <= 0) {
      changed = true;
      return null;
    }

    const quantity = Math.min(item.quantity, product.quantity);
    if (quantity !== item.quantity || item.product !== product) {
      changed = true;
    }

    return {
      productId: product.id,
      product,
      quantity
    };
  }).filter(Boolean);

  if (changed) {
    saveCart();
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
          ${formatMoney(item.product.price)} × ${item.quantity}
        </p>
      </div>

      <div style="display: flex; align-items: center; gap: 15px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <button onclick="updateQuantity('${item.productId}', ${item.quantity - 1})"
            style="background: var(--charcoal); border: 1px solid var(--white); color: var(--white); width: 30px; height: 30px; cursor: pointer; font-size: 1.2rem;">-</button>

          <span style="color: var(--white); font-size: 1.1rem; min-width: 30px; text-align: center;">
            ${item.quantity}
          </span>

          <button onclick="updateQuantity('${item.productId}', ${item.quantity + 1})"
            style="background: var(--charcoal); border: 1px solid var(--white); color: var(--white); width: 30px; height: 30px; cursor: pointer; font-size: 1.2rem;">+</button>
        </div>

        <div style="min-width: 120px; text-align: right; color: var(--soviet-red); font-size: 1.2rem; font-weight: bold;">
          ${formatMoney(item.product.price * item.quantity)}
        </div>

        <button onclick="removeFromCart('${item.productId}')"
          style="background: transparent; border: 2px solid var(--soviet-red); color: var(--soviet-red); padding: 8px 15px; cursor: pointer; text-transform: uppercase; font-family: var(--font-heading); letter-spacing: 1px; font-size: 0.85rem;">
          Удалить
        </button>
      </div>
    </div>
  `).join('');
}

// ========== RENDER POLICY SELECTORS ==========

function renderPolicySelectors() {
  const shippingSelect = document.getElementById('shipping-select');
  if (!shippingSelect) return;

  if (shippingPolicies.length === 0) {
    shippingSelect.disabled = true;
    shippingSelect.innerHTML = '<option value="none">Нет доступных способов доставки</option>';
    return;
  }

  shippingSelect.disabled = false;
  shippingSelect.classList.remove('hidden');
  shippingSelect.innerHTML = shippingPolicies.map(shipping => `
    <option value="${shipping.id}">
      ${shipping.name}
    </option>
  `).join('');
  if (selectedShipping) {
    shippingSelect.value = selectedShipping.id;
  }
}

// ========== CART ACTIONS ==========

function updateQuantity(productId, newQuantity) {
  if (newQuantity <= 0) {
    removeFromCart(productId);
    return;
  }

  const item = cart.find(i => i.productId === productId);
  if (item) {
    if (newQuantity > item.product.quantity) {
      alert(`На складе только ${item.product.quantity} шт.`);
      return;
    }

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
  if (cartCountEl) cartCountEl.textContent = totalItems;
}

function formatMoney(value) {
  return `₸${Math.round(value).toLocaleString()}`;
}

function resetTotals() {
  const promoInfo = document.getElementById('promo-info');
  if (promoInfo) promoInfo.textContent = 'Добавьте товары, чтобы применить скидку.';

  const ids = ['items-total', 'discount-amount', 'tax-amount', 'shipping-cost', 'total-amount'];
  ids.forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = id === 'discount-amount' ? '-₸0' : id === 'total-amount' ? '₸0' : '+₸0';
  });

  const itemsTotal = document.getElementById('items-total');
  if (itemsTotal) itemsTotal.textContent = '₸0';
}

// ========== CALCULATE TOTAL ==========

async function calculateTotal() {
  if (cart.length === 0 || !selectedShipping) {
    resetTotals();
    return;
  }

  try {
    const checkout = await ShopAPI.checkout(
      cart,
      'auto',
      selectedTax ? selectedTax.id : 'progressive',
      selectedShipping.id
    );

    selectedPromotion = checkout.appliedPromotion || promotions.find(p => p.id === checkout.promotionId) || null;

    const promoInfo = document.getElementById('promo-info');
    if (!selectedPromotion || selectedPromotion.id === 'none') {
      promoInfo.textContent = 'Без скидок (но мы старались).';
    } else {
      promoInfo.textContent = `Промо: ${selectedPromotion.name} (−${formatMoney(checkout.discountAmount)})`;
    }

    document.getElementById('items-total').textContent = formatMoney(checkout.itemsTotal);
    document.getElementById('discount-amount').textContent = `-${formatMoney(checkout.discountAmount)}`;
    document.getElementById('tax-amount').textContent = `+${formatMoney(checkout.taxAmount)}`;
    document.getElementById('shipping-cost').textContent = `+${formatMoney(checkout.shippingCost)}`;
    document.getElementById('total-amount').textContent = formatMoney(checkout.total);
  } catch (error) {
    console.error('Error calculating total:', error);
    alert(error.message || 'Ошибка расчёта заказа.');
  }
}

// ========== DELIVERY TRACKING MODAL ==========

function showPigeonTracking() {
    const modal = document.getElementById('pigeon-modal');
    const modalTitle = document.getElementById('modal-title');
    const modalSubtitle = document.getElementById('modal-subtitle');
    const status = document.getElementById('pigeon-status');
    const closeBtn = document.getElementById('close-pigeon-modal');
    const mapContainer = document.getElementById('pigeon-map');

    const isLehaDelivery = selectedShipping && selectedShipping.id === 'leha-delivery';
    const deliveryName = isLehaDelivery ? 'Лёха' : 'Голубь';

    if (isLehaDelivery) {
        modalTitle.innerHTML = 'Лёха уже идёт! Ура!';
        modalSubtitle.textContent = 'Лёха выдвинулся, держите телефон рядом.';
    } else {
        modalTitle.innerHTML = 'Голубь уже летит! Ура!';
        modalSubtitle.textContent = 'Наш пернатый экспресс в пути. Не забудьте выглянуть в окно.';
    }

    modal.classList.remove('hidden');
    mapContainer.innerHTML = '';

    const routeDiv = document.createElement('div');
    routeDiv.style.cssText = `
        position: relative;
        width: 100%;
        height: 100%;
        background: #1C1C1C;
        border: 2px solid var(--concrete);
        overflow: hidden;
    `;

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
    `;
    shopMarker.textContent = 'Маг';
    routeDiv.appendChild(shopMarker);

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
    `;
    homeMarker.textContent = 'Дом';
    routeDiv.appendChild(homeMarker);

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
        deliveryMarker.innerHTML = `
            <div style="
                width: 45px;
                height: 45px;
                border-radius: 50%;
                overflow: hidden;
                border: 3px solid #FFD700;
                background: white;
            ">
                <img src="assets/images/leha.png" style="width: 100%; height: 100%; object-fit: cover;">
            </div>
        `;
    } else {
        deliveryMarker.innerHTML = `
            <div style="
                font-size: 40px;
                text-shadow: 0 0 15px rgba(255, 255, 255, 1);
            ">🕊</div>
        `;
    }

    routeDiv.appendChild(deliveryMarker);
    mapContainer.appendChild(routeDiv);

    let progress = 0;
    status.textContent = `Статус: ${deliveryName} выехал из магазина...`;

    function animate() {
        progress += 0.4;

        if (progress >= 100) {
            progress = 100;
            status.textContent = `Статус: ${deliveryName} уже у двери!`;
            return;
        }

        const currentLeft = 10 + (80 * progress / 100);
        deliveryMarker.style.left = `${currentLeft}%`;

        if (progress > 25 && progress < 27) {
            status.textContent = `Статус: ${deliveryName} пролетает парк...`;
        } else if (progress > 50 && progress < 52) {
            status.textContent = `Статус: ${deliveryName} на полпути!`;
        } else if (progress > 75 && progress < 77) {
            status.textContent = `Статус: ${deliveryName} почти у цели!`;
        }

        setTimeout(animate, 100);
    }

    animate();

    closeBtn.onclick = () => {
        modal.classList.add('hidden');
        window.location.href = 'index.html';
    };
}

// ========== EVENT LISTENERS ==========

function initEventListeners() {
  const shippingSelect = document.getElementById('shipping-select');
  if (shippingSelect) {
    shippingSelect.addEventListener('change', (e) => {
      selectedShipping = shippingPolicies.find(s => s.id === e.target.value) || shippingPolicies[0];
      calculateTotal();
    });
  }

  document.getElementById('checkout-btn').addEventListener('click', () => {
    if (cart.length === 0) {
      alert('Корзина пуста!');
      return;
    }
    showPigeonTracking();
    clearCart();
    const cartCountEl = document.getElementById('cart-count');
    if (cartCountEl) cartCountEl.textContent = '0';
  });
}
