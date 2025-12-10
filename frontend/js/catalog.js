/**
 * Catalog JS - Логика каталога товаров
 * Стиль "Кино" - Простота и атмосфера
 */

let allProducts = [];
let filteredProducts = [];
let currentCategory = 'all';
let currentSort = 'name-asc';
let searchQuery = '';

// ========== INIT ==========

document.addEventListener('DOMContentLoaded', async () => {
  // Загрузить товары
  await loadProducts();

  // Обновить счётчик корзины
  updateCartCount();

  // Инициализировать обработчики
  initEventListeners();
});

// ========== LOAD PRODUCTS ==========

async function loadProducts() {
  const loader = document.getElementById('loader');
  const productGrid = document.getElementById('product-grid');
  const noProducts = document.getElementById('no-products');

  try {
    loader.classList.remove('hidden');
    productGrid.innerHTML = '';
    noProducts.classList.add('hidden');

    allProducts = await ShopAPI.getProducts();
    filteredProducts = [...allProducts];

    applyFilters();
    renderProducts();

    loader.classList.add('hidden');
  } catch (error) {
    console.error('Error loading products:', error);
    loader.classList.add('hidden');
    alert('Ошибка загрузки товаров. Проверьте соединение.');
  }
}

// ========== FILTERS & SORT ==========

function applyFilters() {
  filteredProducts = allProducts.filter(product => {
    // Фильтр по категории
    if (currentCategory !== 'all' && product.category !== currentCategory) {
      return false;
    }

    // Поиск по названию
    if (searchQuery && !product.name.toLowerCase().includes(searchQuery.toLowerCase())) {
      return false;
    }

    return true;
  });

  // Сортировка
  filteredProducts.sort((a, b) => {
    switch (currentSort) {
      case 'name-asc':
        return a.name.localeCompare(b.name);
      case 'name-desc':
        return b.name.localeCompare(a.name);
      case 'price-asc':
        return a.price - b.price;
      case 'price-desc':
        return b.price - a.price;
      default:
        return 0;
    }
  });
}

// ========== RENDER ==========

function renderProducts() {
  const productGrid = document.getElementById('product-grid');
  const noProducts = document.getElementById('no-products');

  if (filteredProducts.length === 0) {
    productGrid.innerHTML = '';
    noProducts.classList.remove('hidden');
    return;
  }

  noProducts.classList.add('hidden');

  productGrid.innerHTML = filteredProducts.map(product => `
    <div class="product-card">
      <img src="${product.image}" alt="${product.name}">
      <h3>${product.name}</h3>
      <p style="color: var(--concrete); font-size: 0.9rem; min-height: 60px;">
        ${product.description.substring(0, 80)}${product.description.length > 80 ? '...' : ''}
      </p>
      <div class="price">₽${product.price.toLocaleString()}</div>
      <div class="stock-status ${getStockStatusClass(product.stockStatus)}">
        ${getStockStatusText(product.stockStatus)}
      </div>
      <button
        class="btn-stamp"
        style="width: 100%; margin-top: 10px;"
        onclick="addToCart('${product.id}')"
        ${product.stockStatus === 'OUT_OF_STOCK' ? 'disabled' : ''}
      >
        ${product.stockStatus === 'OUT_OF_STOCK' ? 'НЕТ В НАЛИЧИИ' : 'КУПИТЬ'}
      </button>
    </div>
  `).join('');
}

function getStockStatusClass(status) {
  switch (status) {
    case 'IN_STOCK':
      return 'in-stock';
    case 'LOW':
      return 'low';
    case 'OUT_OF_STOCK':
      return 'out';
    default:
      return '';
  }
}

function getStockStatusText(status) {
  switch (status) {
    case 'IN_STOCK':
      return 'В НАЛИЧИИ';
    case 'LOW':
      return 'ЗАКАНЧИВАЕТСЯ';
    case 'OUT_OF_STOCK':
      return 'НЕТ В НАЛИЧИИ';
    default:
      return 'НЕИЗВЕСТНО';
  }
}

// ========== CART ==========

function addToCart(productId) {
  // Получить текущую корзину из localStorage
  let cart = JSON.parse(localStorage.getItem('cart') || '[]');

  // Проверить, есть ли уже этот товар
  const existingItem = cart.find(item => item.productId === productId);

  if (existingItem) {
    existingItem.quantity += 1;
  } else {
    const product = allProducts.find(p => p.id === productId);
    if (product) {
      cart.push({
        productId: product.id,
        product: product,
        quantity: 1
      });
    }
  }

  // Сохранить корзину
  localStorage.setItem('cart', JSON.stringify(cart));

  // Обновить счётчик
  updateCartCount();

  // Показать уведомление (простая анимация)
  showNotification('Товар добавлен в корзину!');
}

function updateCartCount() {
  const cart = JSON.parse(localStorage.getItem('cart') || '[]');
  const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
  const cartCountEl = document.getElementById('cart-count');
  if (cartCountEl) {
    cartCountEl.textContent = `(${totalItems})`;
  }
}

function showNotification(message) {
  // Создать временное уведомление
  const notification = document.createElement('div');
  notification.textContent = message;
  notification.style.cssText = `
    position: fixed;
    top: 100px;
    right: 30px;
    background: var(--soviet-red);
    color: var(--white);
    padding: 20px 30px;
    font-family: var(--font-heading);
    text-transform: uppercase;
    letter-spacing: 2px;
    box-shadow: 5px 5px 15px rgba(0, 0, 0, 0.7);
    z-index: 9999;
    animation: slideIn 0.3s ease;
  `;

  document.body.appendChild(notification);

  // Удалить через 2 секунды
  setTimeout(() => {
    notification.style.animation = 'slideOut 0.3s ease';
    setTimeout(() => notification.remove(), 300);
  }, 2000);
}

// ========== EVENT LISTENERS ==========

function initEventListeners() {
  // Категории
  const categoryBtns = document.querySelectorAll('.category-tabs .btn-stamp');
  categoryBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      // Удалить active у всех
      categoryBtns.forEach(b => b.classList.remove('active'));

      // Добавить active к текущей
      e.target.classList.add('active');

      // Применить фильтр
      currentCategory = e.target.dataset.category;
      applyFilters();
      renderProducts();
    });
  });

  // Поиск
  const searchInput = document.getElementById('search-input');
  searchInput.addEventListener('input', (e) => {
    searchQuery = e.target.value;
    applyFilters();
    renderProducts();
  });

  // Сортировка
  const sortSelect = document.getElementById('sort-select');
  sortSelect.addEventListener('change', (e) => {
    currentSort = e.target.value;
    applyFilters();
    renderProducts();
  });
}

// CSS анимации для уведомлений
const style = document.createElement('style');
style.textContent = `
  @keyframes slideIn {
    from {
      transform: translateX(400px);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }

  @keyframes slideOut {
    from {
      transform: translateX(0);
      opacity: 1;
    }
    to {
      transform: translateX(400px);
      opacity: 0;
    }
  }
`;
document.head.appendChild(style);
