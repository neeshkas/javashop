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
  loadCart();
  await loadPolicies();
  updateCartCount();
  renderCart();
  renderPolicySelectors();
  initEventListeners();
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

    selectedTax = taxes.find(t => t.id === 'progressive') || taxes[0];
    selectedShipping = shippingPolicies[0] || null;
    selectedPromotion = promotions[0] || null;
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
          ₸${(item.product.price * item.quantity).toLocaleString()}
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
  shippingSelect.disabled = false;
  shippingSelect.classList.remove('hidden');
  shippingSelect.innerHTML = shippingPolicies.map(shipping => `
    <option value="${shipping.id}">
      ${shipping.id.includes('pigeon') || shipping.id === 'express' ? '🕊 ' : ''}${shipping.name}
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

// ========== CALCULATE TOTAL ==========

async function calculateTotal() {
  if (cart.length === 0 || !selectedShipping) {
    return;
  }

  try {
    let bestPromotion = promotions[0];
    let maxDiscount = 0;

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

    const checkout = await ShopAPI.checkout(
      cart,
      selectedPromotion.id,
      selectedTax.id,
      selectedShipping.id
    );

    const promoInfo = document.getElementById('promo-info');
    if (selectedPromotion.id === 'none') {
      promoInfo.textContent = 'Без скидок (но мы старались).';
    } else {
      promoInfo.textContent = `Промо: ${selectedPromotion.name} (−₸${checkout.discountAmount.toLocaleString()})`;
    }

    document.getElementById('items-total').textContent = `₸${checkout.itemsTotal.toLocaleString()}`;
    document.getElementById('discount-amount').textContent = `-₸${checkout.discountAmount.toLocaleString()}`;
    document.getElementById('tax-amount').textContent = `+₸${checkout.taxAmount.toLocaleString()}`;
    document.getElementById('shipping-cost').textContent = `+₸${checkout.shippingCost.toLocaleString()}`;
    document.getElementById('total-amount').textContent = `₸${checkout.total.toLocaleString()}`;
  } catch (error) {
    console.error('Error calculating total:', error);
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
    if (cartCountEl) cartCountEl.textContent = '(0)';
  });
}
