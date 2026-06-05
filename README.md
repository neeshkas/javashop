# Javashop

Учебный музыкальный интернет-магазин на Java backend и статическом frontend.

Один Java-сервер отдаёт сразу всё приложение:

- frontend: `http://localhost:8086/`
- backend API: `http://localhost:8086/api/*`

## Быстрый запуск через Docker

Это рекомендуемый способ запуска: Docker поднимает сразу frontend и backend в одном контейнере.

Из корня проекта:

```bash
docker compose up --build
```

После запуска откройте:

```text
http://localhost:8086/
```

Корзина:

```text
http://localhost:8086/cart.html
```

Health-check:

```text
http://localhost:8086/api/health
```

Запуск в фоне:

```bash
docker compose up --build -d
```

Остановка:

```bash
docker compose down
```

## Локальный запуск без Docker

Нужен JDK с командой `java`.

Проверить:

```bash
java -version
```

Запуск:

```bash
java src/server/ProductApiServer.java
```

Откройте:

```text
http://localhost:8086/
```

Проект не использует Maven, Gradle или npm.

## Запуск на другом порту

Для локального Java-запуска:

```bash
PORT=8090 java src/server/ProductApiServer.java
```

Для Docker-запуска измените проброс порта в `docker-compose.yml`, например:

```yaml
ports:
  - "8090:8086"
```

После этого откройте:

```text
http://localhost:8090/
```

## Проверка API

```bash
curl http://localhost:8086/api/health
curl http://localhost:8086/api/products
curl http://localhost:8086/api/promotions
curl http://localhost:8086/api/taxes
curl http://localhost:8086/api/shipping-policies
```

Проверка checkout:

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

## Если порт занят

Остановите старый контейнер:

```bash
docker compose down
```

Или используйте другой порт, изменив `ports` в `docker-compose.yml`.

## Важно

- Не открывайте `frontend/index.html` напрямую, если хотите проверить обычный сценарий работы.
- Товары и справочники хранятся в памяти процесса.
- Создание, обновление и удаление товаров через API не сохраняются после перезапуска сервера.
- Подробная техническая документация находится в `techdocs.md`.
