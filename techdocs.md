# Javashop Technical Documentation

## Назначение проекта

Javashop - учебный интернет-магазин музыкальных товаров. Проект показывает связку простого Java backend без внешних фреймворков и статического frontend на HTML/CSS/JavaScript.

Основной пользовательский сценарий:

1. Пользователь открывает каталог.
2. Frontend запрашивает товары у backend.
3. Пользователь добавляет товары в корзину.
4. Корзина хранится в `localStorage`, но перед расчётом синхронизируется с backend.
5. Frontend отправляет состав корзины на backend.
6. Backend рассчитывает скидку, налог, доставку и итоговую сумму.
7. Frontend показывает результат пользователю.

## Технологический стек

Backend:

- Java.
- Встроенный HTTP-сервер JDK: `com.sun.net.httpserver.HttpServer`.
- Данные хранятся in-memory в `ProductApiServer`.
- JSON формируется вручную.
- Для входящих JSON-запросов используется небольшой встроенный parser внутри `ProductApiServer`.
- Внешних backend-зависимостей нет.
- Maven/Gradle в проекте нет.

Container:

- `Dockerfile` с multi-stage сборкой.
- Build stage использует JDK и компилирует `ProductApiServer.java`.
- Runtime stage использует JRE и запускает `server.ProductApiServer`.
- `docker-compose.yml` публикует приложение на `localhost:8086`.

Frontend:

- HTML.
- CSS без UI-фреймворков.
- Vanilla JavaScript.
- `fetch` для HTTP-запросов.
- `localStorage` для хранения корзины на стороне браузера.

IDE/project:

- IntelliJ IDEA Java module: `shoppppp.iml`.
- Настройки IDEA лежат в `.idea/`.
- Проект настроен под JDK 24 в `.idea/misc.xml`, но фактически может запускаться современным JDK с поддержкой source-file mode.

## Структура проекта

```text
.
├── frontend/
│   ├── index.html          # Каталог
│   ├── cart.html           # Корзина и checkout
│   ├── css/main.css        # Общие стили
│   ├── js/api.js           # API wrapper для backend
│   ├── js/catalog.js       # Логика каталога
│   ├── js/cart.js          # Логика корзины
│   └── assets/images/      # Локальные изображения frontend
├── src/
│   ├── server/
│   │   └── ProductApiServer.java
│   ├── product/            # Учебная доменная модель товаров
│   ├── product/pricing/    # Политики скидок
│   ├── product/shipping/   # Политики доставки
│   ├── product/tax/        # Политики налогов
│   └── category/
├── static/                 # Файлы, отдаваемые backend через /static/*
├── Dockerfile              # Сборка Docker-образа
├── docker-compose.yml      # Запуск приложения в контейнере
├── reqs/                   # Требования/задания
├── techdocs.md             # Этот документ
└── README.md               # Инструкция по запуску
```

## Главные модули

### `src/server/ProductApiServer.java`

Это главный backend-сервис. Он:

- поднимает HTTP-сервер;
- регистрирует API endpoints;
- отдаёт статический frontend;
- хранит товары, промо-акции, налоги и способы доставки;
- валидирует входящие JSON-запросы;
- рассчитывает checkout;
- отдаёт JSON-ответы.

Порт по умолчанию: `8086`.

Порт можно изменить переменной окружения:

```bash
PORT=8090 java src/server/ProductApiServer.java
```

### Docker

Docker-запуск использует тот же backend-процесс. Отдельного nginx или отдельного frontend-сервиса нет, потому что Java-сервер уже отдаёт статический frontend и API.

```text
Docker container
  └── Java HttpServer
      ├── /                 -> frontend
      ├── /static/*         -> static
      └── /api/*            -> backend API
```

Команда запуска:

```bash
docker compose up --build
```

### `frontend/js/api.js`

Единая точка общения frontend с backend.

Определяет `API_BASE`:

- если страница открыта через `file://`, используется `http://localhost:8086/api`;
- если страница открыта через сервер, используется `${window.location.origin}/api`.

Методы:

- `ShopAPI.getProducts()`
- `ShopAPI.getProduct(id)`
- `ShopAPI.createProduct(productData)`
- `ShopAPI.updateProduct(id, productData)`
- `ShopAPI.deleteProduct(id)`
- `ShopAPI.getPromotions()`
- `ShopAPI.getTaxPolicies()`
- `ShopAPI.getShippingPolicies()`
- `ShopAPI.checkout(cartItems, promotionId, taxPolicyId, shippingPolicyId)`

### `frontend/js/catalog.js`

Отвечает за каталог:

- загружает товары через `ShopAPI.getProducts()`;
- фильтрует товары по категории;
- ищет по названию;
- сортирует по названию/цене;
- добавляет товары в корзину;
- проверяет остаток на складе перед добавлением.

Корзина в каталоге хранится в `localStorage` в ключе `cart`.

### `frontend/js/cart.js`

Отвечает за корзину:

- читает `cart` из `localStorage`;
- загружает справочники промо, налогов и доставки;
- синхронизирует товары с backend через `ShopAPI.getProducts()`;
- ограничивает количество товара складским остатком;
- отправляет checkout на backend через `ShopAPI.checkout(...)`;
- отображает итоговую сумму, скидку, налог и доставку.

Frontend больше не считает итоговую стоимость самостоятельно. Он только отправляет состав корзины и показывает ответ backend.

## Как сервисы общаются

В проекте один backend-процесс:

```text
Browser
  |
  | GET /
  v
Java HttpServer
  |
  | отдаёт frontend/index.html, css, js
  v
Browser
  |
  | fetch('/api/products')
  | fetch('/api/promotions')
  | fetch('/api/taxes')
  | fetch('/api/shipping-policies')
  | fetch('/api/checkout')
  v
Java HttpServer API
```

Frontend и API обслуживаются одним сервером и одним origin, если открывать проект через `http://localhost:8086/`. Поэтому CORS обычно не нужен. CORS всё равно включён для API, чтобы frontend мог работать и при открытии HTML через `file://`.

## API endpoints

Base URL:

```text
http://localhost:8086/api
```

### Health

```http
GET /api/health
```

Ответ:

```json
{
  "status": "ok",
  "service": "javashop",
  "port": 8086
}
```

### Products

```http
GET /api/products
```

Возвращает массив товаров.

```http
GET /api/products/{id}
```

Возвращает один товар или `404`.

```http
POST /api/products
Content-Type: application/json
```

Создаёт товар.

```json
{
  "id": "13",
  "name": "Новый товар",
  "description": "Описание",
  "price": 10000,
  "quantity": 5,
  "category": "merch",
  "type": "physical",
  "image": "https://example.com/image.jpg"
}
```

```http
PUT /api/products/{id}
Content-Type: application/json
```

Обновляет товар. Если поле не передано, backend сохраняет текущее значение.

```http
DELETE /api/products/{id}
```

Удаляет товар.

Модель товара:

```json
{
  "id": "1",
  "name": "Электрогитара \"Fender Telecaster\"",
  "description": "Очень классная гитара. Купи пж. Реально классная.",
  "price": 250000,
  "quantity": 5,
  "category": "guitars",
  "stockStatus": "LOW",
  "type": "physical",
  "image": "https://images.unsplash.com/..."
}
```

`stockStatus` вычисляется backend:

- `OUT_OF_STOCK`, если `quantity == 0`;
- `LOW`, если `quantity <= 10`;
- `IN_STOCK`, если `quantity > 10`.

### Promotions

```http
GET /api/promotions
```

Возвращает справочник промо-акций.

Текущие типы:

- `none`
- `percentage`
- `fixed`
- `bogo-half`
- `buy3-pay2`

### Taxes

```http
GET /api/taxes
```

Возвращает справочник налоговых политик.

Текущие политики:

- `no-tax`
- `flat-vat-12`
- `flat-vat-5`
- `progressive`

`progressive` считается так:

- до `10_000` включительно: 5%;
- до `50_000` включительно: 10%;
- свыше `50_000`: 15%.

### Shipping

```http
GET /api/shipping-policies
```

Возвращает справочник способов доставки.

Текущие способы:

- `none`
- `pigeon-standard`
- `pigeon-express`
- `pigeon-vip`
- `leha-delivery`
- `free-over-50k`

Если в корзине только цифровые товары, доставка считается `0`, даже если выбран платный способ.

### Checkout

```http
POST /api/checkout
Content-Type: application/json
```

Запрос:

```json
{
  "items": [
    { "productId": "1", "quantity": 2 },
    { "productId": "2", "quantity": 3 }
  ],
  "promotionId": "auto",
  "taxPolicyId": "progressive",
  "shippingPolicyId": "free-over-50k"
}
```

`promotionId: "auto"` означает, что backend сам выбирает промо с минимальной итоговой стоимостью товаров до налога и доставки.

Ответ:

```json
{
  "itemBreakdown": [
    {
      "productId": "1",
      "productName": "Электрогитара \"Fender Telecaster\"",
      "quantity": 2,
      "basePrice": 500000,
      "priceWithPromo": 375000,
      "discount": 125000
    }
  ],
  "itemsTotal": 575000,
  "subtotal": 437500,
  "discountAmount": 137500,
  "promotion": "Второй товар -50%",
  "promotionId": "bogo-half",
  "appliedPromotion": {
    "id": "bogo-half",
    "name": "Второй товар -50%",
    "type": "bogo-half",
    "value": null
  },
  "taxAmount": 65625,
  "taxPolicy": "Прогрессивный НДС",
  "taxPolicyId": "progressive",
  "shippingCost": 0,
  "shippingPolicy": "Бесплатная доставка при заказе > 50,000₸",
  "shippingPolicyId": "free-over-50k",
  "requiresShipping": true,
  "total": 503125
}
```

Ошибки checkout:

- товар не найден;
- количество меньше или равно нулю;
- количество больше остатка;
- неизвестная скидка/налог/доставка;
- некорректный JSON.

## Логика checkout

Backend выполняет расчёт в таком порядке:

1. Валидирует `items`.
2. Находит товары в in-memory каталоге.
3. Проверяет, что `quantity` не превышает складской остаток.
4. Выбирает промо:
   - если `promotionId == "auto"`, перебирает все промо и выбирает минимальный subtotal;
   - иначе применяет конкретное промо.
5. Считает:
   - `itemsTotal` - сумма без скидок;
   - `subtotal` - сумма после скидок;
   - `discountAmount` - разница между ними.
6. Считает налог от `subtotal`.
7. Считает доставку:
   - `0`, если нет физических товаров;
   - `0`, если выбран самовывоз;
   - `0`, если выбран `free-over-50k` и subtotal >= 50000;
   - иначе стоимость выбранного способа доставки.
8. Возвращает `total = subtotal + taxAmount + shippingCost`.

## Хранение данных

Сейчас данные не сохраняются в файл или базу данных.

Все товары и справочники создаются при старте сервера:

- `seedProducts()`
- `seedPromotions()`
- `seedTaxes()`
- `seedShippingPolicies()`

`POST`, `PUT`, `DELETE` меняют данные только в памяти текущего процесса. После перезапуска сервера изменения пропадут.

Если нужна постоянная база, ближайший логичный шаг - вынести хранилище из `ProductApiServer` в отдельный repository/service слой и подключить SQLite/PostgreSQL.

## Доменная Java-модель

В `src/product` есть учебная ООП-модель:

- `Product`
- `PhysicalProduct`
- `DigitalProduct`
- `PricePolicy`
- `Promotion`
- `ShippingPolicy`
- `TaxPolicy`

Эти классы демонстрируют наследование, интерфейсы и полиморфизм. Текущий HTTP API хранит собственные record-модели внутри `ProductApiServer`, чтобы не усложнять серверную часть внешними mapper-слоями.

Если проект будет развиваться, лучше привести API и доменную модель к одной общей модели данных.

## Статические файлы

Java-сервер отдаёт:

- `/` -> `frontend/index.html`;
- `/cart.html` -> `frontend/cart.html`;
- `/css/main.css` -> `frontend/css/main.css`;
- `/js/api.js` -> `frontend/js/api.js`;
- `/assets/images/*` -> `frontend/assets/images/*`;
- `/static/*` -> `static/*`.

`/static/*` используется для изображений, которые должен отдавать backend.

## Кодировка

В проекте много русскоязычного текста. Файлы должны сохраняться в UTF-8.

## Что важно знать новому разработчику

- Запускать лучше через `http://localhost:8086/`, а не открывать HTML напрямую.
- Backend сейчас монолитный и простой: вся API-логика в `ProductApiServer`.
- Frontend не содержит бизнес-расчётов checkout.
- Корзина хранится в браузере, но цены и остатки проверяются backend.
- Данные in-memory, поэтому CRUD не переживает перезапуск сервера.
- В проекте нет package manager и внешних зависимостей.
- Проверки удобно делать через `curl` или небольшой скрипт на Python/Node.

## Быстрая проверка API

```bash
curl http://localhost:8086/api/health
curl http://localhost:8086/api/products
curl http://localhost:8086/api/promotions
curl http://localhost:8086/api/taxes
curl http://localhost:8086/api/shipping-policies
```

Checkout:

```bash
curl -X POST http://localhost:8086/api/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      { "productId": "1", "quantity": 2 },
      { "productId": "2", "quantity": 3 }
    ],
    "promotionId": "auto",
    "taxPolicyId": "progressive",
    "shippingPolicyId": "free-over-50k"
  }'
```
