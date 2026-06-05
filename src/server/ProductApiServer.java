package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server for the shop.
 *
 * API:
 *  - GET    /api/health
 *  - GET    /api/products
 *  - POST   /api/products
 *  - GET    /api/products/{id}
 *  - PUT    /api/products/{id}
 *  - DELETE /api/products/{id}
 *  - GET    /api/promotions
 *  - GET    /api/taxes
 *  - GET    /api/shipping-policies
 *  - POST   /api/checkout
 *
 * Static:
 *  - / serves files from ./frontend
 *  - /static/* serves files from ./static
 */
public class ProductApiServer {

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8086"));
    private static final Path FRONTEND_ROOT = Path.of("frontend").toAbsolutePath().normalize();
    private static final Path STATIC_ROOT = Path.of("static").toAbsolutePath().normalize();

    private static final List<ApiProduct> PRODUCTS = new ArrayList<>(seedProducts());
    private static final List<PromotionDefinition> PROMOTIONS = seedPromotions();
    private static final List<TaxDefinition> TAXES = seedTaxes();
    private static final List<ShippingDefinition> SHIPPING_POLICIES = seedShippingPolicies();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/health", ProductApiServer::handleHealth);
        server.createContext("/api/products", ProductApiServer::handleProducts);
        server.createContext("/api/promotions", exchange -> handleList(exchange, toJsonPromotions()));
        server.createContext("/api/taxes", exchange -> handleList(exchange, toJsonTaxes()));
        server.createContext("/api/shipping-policies", exchange -> handleList(exchange, toJsonShippingPolicies()));
        server.createContext("/api/checkout", ProductApiServer::handleCheckout);
        server.createContext("/static", exchange -> handleStaticFile(exchange, STATIC_ROOT, "/static"));
        server.createContext("/", ProductApiServer::handleFrontendFile);
        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println("Shop server started on http://localhost:" + PORT);
        System.out.println("Catalog: http://localhost:" + PORT + "/");
        System.out.println("API health: http://localhost:" + PORT + "/api/health");
        server.start();
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmpty(exchange, 204);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonError(exchange, 405, "Method Not Allowed");
            return;
        }

        sendJson(exchange, 200, "{"
                + prop("status", "ok") + ','
                + prop("service", "javashop") + ','
                + rawProp("port", String.valueOf(PORT))
                + "}");
    }

    private static void handleProducts(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmpty(exchange, 204);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/api/products") && !path.startsWith("/api/products/")) {
            sendJsonError(exchange, 404, "Not Found");
            return;
        }

        if (path.equals("/api/products")) {
            switch (exchange.getRequestMethod().toUpperCase(Locale.ROOT)) {
                case "GET" -> sendJson(exchange, 200, toJsonProducts(snapshotProducts()));
                case "POST" -> createProduct(exchange);
                default -> sendJsonError(exchange, 405, "Method Not Allowed");
            }
            return;
        }

        String id = decodePathSegment(path.substring("/api/products/".length()));
        if (id.isBlank() || id.contains("/")) {
            sendJsonError(exchange, 404, "Not Found");
            return;
        }

        switch (exchange.getRequestMethod().toUpperCase(Locale.ROOT)) {
            case "GET" -> getProduct(exchange, id);
            case "PUT" -> updateProduct(exchange, id);
            case "DELETE" -> deleteProduct(exchange, id);
            default -> sendJsonError(exchange, 405, "Method Not Allowed");
        }
    }

    private static void handleList(HttpExchange exchange, String json) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmpty(exchange, 204);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonError(exchange, 405, "Method Not Allowed");
            return;
        }

        sendJson(exchange, 200, json);
    }

    private static void handleCheckout(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmpty(exchange, 204);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            Map<String, Object> payload = parseObjectBody(exchange);
            CheckoutResult result = calculateCheckout(payload);
            sendJson(exchange, 200, toJsonCheckout(result));
        } catch (BadRequestException e) {
            sendJsonError(exchange, 400, e.getMessage());
        }
    }

    private static void createProduct(HttpExchange exchange) throws IOException {
        try {
            Map<String, Object> payload = parseObjectBody(exchange);
            ApiProduct product;
            synchronized (PRODUCTS) {
                product = productFromPayload(payload, null, nextProductId());
                ensureUniqueId(product.id(), null);
                PRODUCTS.add(product);
            }
            sendJson(exchange, 201, toJson(product));
        } catch (BadRequestException e) {
            sendJsonError(exchange, 400, e.getMessage());
        }
    }

    private static void getProduct(HttpExchange exchange, String id) throws IOException {
        ApiProduct product = findProduct(id);
        if (product == null) {
            sendJsonError(exchange, 404, "Product not found");
            return;
        }

        sendJson(exchange, 200, toJson(product));
    }

    private static void updateProduct(HttpExchange exchange, String id) throws IOException {
        try {
            ApiProduct updated;
            synchronized (PRODUCTS) {
                int index = findProductIndex(id);
                if (index < 0) {
                    sendJsonError(exchange, 404, "Product not found");
                    return;
                }

                ApiProduct current = PRODUCTS.get(index);
                Map<String, Object> payload = parseObjectBody(exchange);
                updated = productFromPayload(payload, current, id);
                ensureUniqueId(updated.id(), current.id());
                PRODUCTS.set(index, updated);
            }
            sendJson(exchange, 200, toJson(updated));
        } catch (BadRequestException e) {
            sendJsonError(exchange, 400, e.getMessage());
        }
    }

    private static void deleteProduct(HttpExchange exchange, String id) throws IOException {
        synchronized (PRODUCTS) {
            int index = findProductIndex(id);
            if (index < 0) {
                sendJsonError(exchange, 404, "Product not found");
                return;
            }
            PRODUCTS.remove(index);
        }

        sendJson(exchange, 200, "{"
                + prop("status", "deleted") + ','
                + prop("id", id)
                + "}");
    }

    private static CheckoutResult calculateCheckout(Map<String, Object> payload) {
        List<CheckoutItem> items = parseCheckoutItems(payload);
        String promotionId = optString(payload, "promotionId", "auto");
        String taxPolicyId = optString(payload, "taxPolicyId", "progressive");
        String shippingPolicyId = optString(payload, "shippingPolicyId", "none");

        PromotionDefinition promotion = "auto".equalsIgnoreCase(promotionId)
                ? chooseBestPromotion(items)
                : findPromotion(promotionId);
        TaxDefinition tax = findTax(taxPolicyId);
        ShippingDefinition shipping = findShipping(shippingPolicyId);

        List<CheckoutLine> lines = new ArrayList<>();
        double itemsTotal = 0.0;
        double subtotal = 0.0;
        double digitalSubtotal = 0.0;
        boolean requiresShipping = false;

        for (CheckoutItem item : items) {
            ApiProduct product = item.product();
            int quantity = item.quantity();
            double basePrice = roundMoney(product.price() * quantity);
            double priceWithPromotion = roundMoney(priceWithPromotion(product.price(), quantity, promotion));
            double discount = roundMoney(basePrice - priceWithPromotion);

            itemsTotal += basePrice;
            subtotal += priceWithPromotion;
            if ("digital".equalsIgnoreCase(product.type())) {
                digitalSubtotal += priceWithPromotion;
            } else {
                requiresShipping = true;
            }

            lines.add(new CheckoutLine(product, quantity, basePrice, priceWithPromotion, discount));
        }

        itemsTotal = roundMoney(itemsTotal);
        subtotal = roundMoney(subtotal);
        double discountAmount = roundMoney(itemsTotal - subtotal);
        double taxAmount = roundMoney(calculateTax(subtotal, digitalSubtotal, tax));
        double shippingCost = roundMoney(calculateShipping(subtotal, requiresShipping, shipping));
        double total = roundMoney(subtotal + taxAmount + shippingCost);

        return new CheckoutResult(
                lines,
                itemsTotal,
                subtotal,
                discountAmount,
                promotion,
                taxAmount,
                tax,
                shippingCost,
                shipping,
                total,
                requiresShipping
        );
    }

    private static List<CheckoutItem> parseCheckoutItems(Map<String, Object> payload) {
        Object rawItems = payload.get("items");
        if (!(rawItems instanceof List<?> items)) {
            throw new BadRequestException("Field 'items' must be an array");
        }
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<CheckoutItem> checkoutItems = new ArrayList<>();
        for (Object rawItem : items) {
            if (!(rawItem instanceof Map<?, ?> rawMap)) {
                throw new BadRequestException("Each cart item must be an object");
            }

            Map<String, Object> item = normalizeObject(rawMap);
            String productId = optString(item, "productId", null);
            if (productId == null || productId.isBlank()) {
                throw new BadRequestException("Cart item is missing productId");
            }

            int quantity = optInt(item, "quantity", 1);
            if (quantity <= 0) {
                throw new BadRequestException("Quantity must be greater than zero");
            }

            ApiProduct product = findProduct(productId);
            if (product == null) {
                throw new BadRequestException("Product not found: " + productId);
            }
            if (quantity > product.quantity()) {
                throw new BadRequestException("Not enough stock for product: " + product.name());
            }

            checkoutItems.add(new CheckoutItem(product, quantity));
        }

        return checkoutItems;
    }

    private static PromotionDefinition chooseBestPromotion(List<CheckoutItem> items) {
        PromotionDefinition best = findPromotion("none");
        double bestSubtotal = subtotalWithPromotion(items, best);

        for (PromotionDefinition promotion : PROMOTIONS) {
            double candidate = subtotalWithPromotion(items, promotion);
            if (candidate < bestSubtotal) {
                bestSubtotal = candidate;
                best = promotion;
            }
        }

        return best;
    }

    private static double subtotalWithPromotion(List<CheckoutItem> items, PromotionDefinition promotion) {
        double subtotal = 0.0;
        for (CheckoutItem item : items) {
            subtotal += priceWithPromotion(item.product().price(), item.quantity(), promotion);
        }
        return roundMoney(subtotal);
    }

    private static double priceWithPromotion(double unitPrice, int quantity, PromotionDefinition promotion) {
        double safeUnitPrice = Math.max(0.0, unitPrice);
        int safeQuantity = Math.max(0, quantity);

        return switch (promotion.type()) {
            case "percentage" -> safeUnitPrice * safeQuantity * (1 - promotion.valueOrZero() / 100.0);
            case "fixed" -> Math.max(0.0, safeUnitPrice - promotion.valueOrZero()) * safeQuantity;
            case "bogo-half" -> {
                int pairs = safeQuantity / 2;
                int singles = safeQuantity % 2;
                yield pairs * (safeUnitPrice * 1.5) + singles * safeUnitPrice;
            }
            case "buy3-pay2" -> safeUnitPrice * (safeQuantity - safeQuantity / 3);
            default -> safeUnitPrice * safeQuantity;
        };
    }

    private static double calculateTax(double subtotal, double digitalSubtotal, TaxDefinition tax) {
        if ("no-tax".equals(tax.id())) {
            return 0.0;
        }

        if ("progressive".equals(tax.type())) {
            if (subtotal <= 10_000.0) return subtotal * 0.05;
            if (subtotal <= 50_000.0) return subtotal * 0.10;
            return subtotal * 0.15;
        }

        double taxableAmount = tax.digitalOnly() ? digitalSubtotal : subtotal;
        return taxableAmount * tax.rateOrZero();
    }

    private static double calculateShipping(double subtotal, boolean requiresShipping, ShippingDefinition shipping) {
        if (!requiresShipping || "none".equals(shipping.id())) {
            return 0.0;
        }
        if (shipping.threshold() != null && subtotal >= shipping.threshold()) {
            return 0.0;
        }
        return shipping.cost();
    }

    private static ApiProduct productFromPayload(Map<String, Object> payload, ApiProduct current, String fallbackId) {
        String id = optString(payload, "id", current == null ? fallbackId : current.id());
        String name = optString(payload, "name", current == null ? null : current.name());
        String description = optString(payload, "description", current == null ? "" : current.description());
        double price = optDouble(payload, "price", current == null ? 0.0 : current.price());
        int quantity = optInt(payload, "quantity", current == null ? 0 : current.quantity());
        String category = optString(payload, "category", current == null ? null : current.category());
        String type = optString(payload, "type", current == null ? "physical" : current.type());
        String image = optString(payload, "image", current == null ? "" : current.image());

        if (id == null || id.isBlank()) {
            throw new BadRequestException("Product id is required");
        }
        if (name == null || name.trim().length() < 2) {
            throw new BadRequestException("Product name must contain at least 2 characters");
        }
        if (description != null && description.length() > 500) {
            throw new BadRequestException("Product description is too long");
        }
        if (price < 0.0 || price > 10_000_000.0) {
            throw new BadRequestException("Product price must be between 0 and 10000000");
        }
        if (quantity < 0 || quantity > 1_000_000) {
            throw new BadRequestException("Product quantity must be between 0 and 1000000");
        }

        String normalizedType = type == null ? "physical" : type.trim().toLowerCase(Locale.ROOT);
        if (!normalizedType.equals("physical") && !normalizedType.equals("digital")) {
            throw new BadRequestException("Product type must be either physical or digital");
        }

        return new ApiProduct(
                id.trim(),
                name.trim(),
                description == null ? "" : description.trim(),
                roundMoney(price),
                quantity,
                category == null || category.isBlank() ? null : category.trim(),
                normalizedType,
                image == null ? "" : image.trim()
        );
    }

    private static void ensureUniqueId(String id, String allowedExistingId) {
        for (ApiProduct product : PRODUCTS) {
            if (product.id().equals(id) && !product.id().equals(allowedExistingId)) {
                throw new BadRequestException("Product id already exists: " + id);
            }
        }
    }

    private static List<ApiProduct> snapshotProducts() {
        synchronized (PRODUCTS) {
            return new ArrayList<>(PRODUCTS);
        }
    }

    private static ApiProduct findProduct(String id) {
        synchronized (PRODUCTS) {
            for (ApiProduct product : PRODUCTS) {
                if (product.id().equals(id)) {
                    return product;
                }
            }
        }
        return null;
    }

    private static int findProductIndex(String id) {
        for (int i = 0; i < PRODUCTS.size(); i++) {
            if (PRODUCTS.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static String nextProductId() {
        int max = 0;
        for (ApiProduct product : PRODUCTS) {
            try {
                max = Math.max(max, Integer.parseInt(product.id()));
            } catch (NumberFormatException ignored) {
                // Non-numeric ids are allowed; generated ids simply skip them.
            }
        }
        return String.valueOf(max + 1);
    }

    private static PromotionDefinition findPromotion(String id) {
        for (PromotionDefinition promotion : PROMOTIONS) {
            if (promotion.id().equals(id)) {
                return promotion;
            }
        }
        throw new BadRequestException("Promotion not found: " + id);
    }

    private static TaxDefinition findTax(String id) {
        for (TaxDefinition tax : TAXES) {
            if (tax.id().equals(id)) {
                return tax;
            }
        }
        throw new BadRequestException("Tax policy not found: " + id);
    }

    private static ShippingDefinition findShipping(String id) {
        for (ShippingDefinition shipping : SHIPPING_POLICIES) {
            if (shipping.id().equals(id)) {
                return shipping;
            }
        }
        throw new BadRequestException("Shipping policy not found: " + id);
    }

    private static List<ApiProduct> seedProducts() {
        List<ApiProduct> list = new ArrayList<>();

        list.add(new ApiProduct(
                "1",
                "Электрогитара \"Fender Telecaster\"",
                "Очень классная гитара. Купи пж. Реально классная.",
                250000,
                5,
                "guitars",
                "physical",
                "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "2",
                "Винил \"КИНО - Группа Крови\"",
                "Оригинальный Альбом. Сам Цой писал. Я дописал.",
                25000,
                100,
                "vinyl",
                "digital",
                "https://images.unsplash.com/photo-1619983081563-430f63602796?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "3",
                "Синтезатор Yamaha DX7",
                "Легендарный синтезатор. Если честно, это мой.",
                450000,
                2,
                "synths",
                "physical",
                "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "4",
                "Футболка \"ЦОЙ ЖИВ!!\"",
                "Цой нарисован. А, нет, не нарисован.",
                45000,
                50,
                "merch",
                "physical",
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "5",
                "Винил \"Звезда по имени Солнце\"",
                "Да реально настоящий.",
                35000,
                30,
                "vinyl",
                "digital",
                "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "6",
                "Акустическая гитара Fender",
                "Гитара норм по идее. Там еще HDMI снизу.",
                815000,
                3,
                "guitars",
                "physical",
                "https://images.unsplash.com/photo-1564186763535-ebb21ef5277f?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "7",
                "Плакат \"ЦОЙ ЖИВ!\"",
                "На стену вешать.",
                500,
                200,
                "merch",
                "physical",
                "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "8",
                "Винил \"Перемен!\"",
                "Перемен! Требуют наши сердца!",
                50000,
                5,
                "vinyl",
                "digital",
                "https://images.unsplash.com/photo-1594623930572-300a3011d9ae?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "9",
                "Билет на концерт 2026 года",
                "Пройдет в Алматы. У меня дома.",
                80000,
                15000,
                "tickets",
                "digital",
                "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "10",
                "ВИП места у меня дома.",
                "В туалет сходить сможете.",
                250000,
                2000,
                "tickets",
                "digital",
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "11",
                "Подарочная карта \"ЦОЙ ЖИВ!\"",
                "QR код просто так.",
                1500,
                1000,
                "tickets",
                "digital",
                "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=400&h=300&fit=crop&auto=format&q=80"
        ));

        list.add(new ApiProduct(
                "12",
                "Леха",
                "Красивый мальчик. Возможен торг.",
                999999,
                1,
                "merch",
                "physical",
                "http://localhost:" + PORT + "/static/alexey.jpg"
        ));

        return list;
    }

    private static List<PromotionDefinition> seedPromotions() {
        return List.of(
                new PromotionDefinition("none", "Без скидки", "none", null),
                new PromotionDefinition("percentage-15", "15% на всё", "percentage", 15.0),
                new PromotionDefinition("fixed-5000", "Фикс. скидка 5000₸", "fixed", 5000.0),
                new PromotionDefinition("bogo-half", "Второй товар -50%", "bogo-half", null),
                new PromotionDefinition("buy3-pay2", "3 по цене 2", "buy3-pay2", null)
        );
    }

    private static List<TaxDefinition> seedTaxes() {
        return List.of(
                new TaxDefinition("no-tax", "Без налога", 0.0, "flat", false),
                new TaxDefinition("flat-vat-12", "НДС 12%", 0.12, "flat", false),
                new TaxDefinition("flat-vat-5", "НДС 5% (цифровые товары)", 0.05, "flat", true),
                new TaxDefinition("progressive", "Прогрессивный НДС", null, "progressive", false)
        );
    }

    private static List<ShippingDefinition> seedShippingPolicies() {
        return List.of(
                new ShippingDefinition("none", "Самовывоз (бесплатно)", 0.0, null),
                new ShippingDefinition("pigeon-standard", "Стандартная доставка (2-3 дня)", 800.0, null),
                new ShippingDefinition("pigeon-express", "Экспресс-доставка (в тот же день)", 2500.0, null),
                new ShippingDefinition("pigeon-vip", "VIP доставка с GPS-трекером", 5000.0, null),
                new ShippingDefinition("leha-delivery", "Леха принесет (1-2 часа)", 1500.0, null),
                new ShippingDefinition("free-over-50k", "Бесплатная доставка при заказе > 50,000₸", 0.0, 50_000.0)
        );
    }

    private static void handleFrontendFile(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getPath().startsWith("/api/")) {
            addCors(exchange);
            sendJsonError(exchange, 404, "API endpoint not found");
            return;
        }

        handleStaticFile(exchange, FRONTEND_ROOT, "");
    }

    private static void handleStaticFile(HttpExchange exchange, Path root, String urlPrefix) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String relativePath = urlPrefix.isEmpty() ? path : path.substring(urlPrefix.length());

        if (relativePath.isBlank() || "/".equals(relativePath)) {
            relativePath = "/index.html";
        }

        relativePath = decodePathSegment(relativePath);
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) {
            sendText(exchange, 403, "Forbidden");
            return;
        }
        if (Files.isDirectory(file)) {
            file = file.resolve("index.html").normalize();
        }
        if (!Files.exists(file) || Files.isDirectory(file)) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", guessMimeType(file));
        headers.set("Content-Length", String.valueOf(bytes.length));
        if (root.equals(STATIC_ROOT)) {
            headers.set("Access-Control-Allow-Origin", "*");
        }
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : bytes.length);
        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.close();
        }
    }

    private static Map<String, Object> parseObjectBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return new LinkedHashMap<>();
        }

        Object parsed = JsonParser.parse(body);
        if (!(parsed instanceof Map<?, ?> rawMap)) {
            throw new BadRequestException("Request body must be a JSON object");
        }
        return normalizeObject(rawMap);
    }

    private static Map<String, Object> normalizeObject(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new BadRequestException("JSON object keys must be strings");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static String optString(Map<String, Object> map, String key, String fallback) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (!(value instanceof String stringValue)) {
            throw new BadRequestException("Field '" + key + "' must be a string");
        }
        return stringValue;
    }

    private static int optInt(Map<String, Object> map, String key, int fallback) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (!(value instanceof Number numberValue)) {
            throw new BadRequestException("Field '" + key + "' must be a number");
        }
        double doubleValue = numberValue.doubleValue();
        int intValue = (int) doubleValue;
        if (doubleValue != intValue) {
            throw new BadRequestException("Field '" + key + "' must be an integer");
        }
        return intValue;
    }

    private static double optDouble(Map<String, Object> map, String key, double fallback) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return fallback;
        }
        Object value = map.get(key);
        if (!(value instanceof Number numberValue)) {
            throw new BadRequestException("Field '" + key + "' must be a number");
        }
        return numberValue.doubleValue();
    }

    private static boolean isOptions(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static void addCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Accept");
    }

    private static void sendEmpty(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendJsonError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{"
                + prop("error", message) + ','
                + rawProp("status", String.valueOf(status))
                + "}");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJsonProducts(List<ApiProduct> products) {
        List<String> jsonProducts = new ArrayList<>();
        for (ApiProduct product : products) {
            jsonProducts.add(toJson(product));
        }
        return jsonArray(jsonProducts);
    }

    private static String toJson(ApiProduct product) {
        return "{"
                + prop("id", product.id()) + ','
                + prop("name", product.name()) + ','
                + prop("description", product.description()) + ','
                + rawProp("price", number(product.price())) + ','
                + rawProp("quantity", String.valueOf(product.quantity())) + ','
                + rawProp("category", nullableString(product.category())) + ','
                + prop("stockStatus", product.stockStatus()) + ','
                + prop("type", product.type()) + ','
                + prop("image", product.image())
                + "}";
    }

    private static String toJsonPromotions() {
        List<String> result = new ArrayList<>();
        for (PromotionDefinition promotion : PROMOTIONS) {
            result.add(toJson(promotion));
        }
        return jsonArray(result);
    }

    private static String toJson(PromotionDefinition promotion) {
        return "{"
                + prop("id", promotion.id()) + ','
                + prop("name", promotion.name()) + ','
                + prop("type", promotion.type()) + ','
                + rawProp("value", nullableNumber(promotion.value()))
                + "}";
    }

    private static String toJsonTaxes() {
        List<String> result = new ArrayList<>();
        for (TaxDefinition tax : TAXES) {
            result.add(toJson(tax));
        }
        return jsonArray(result);
    }

    private static String toJson(TaxDefinition tax) {
        return "{"
                + prop("id", tax.id()) + ','
                + prop("name", tax.name()) + ','
                + rawProp("rate", nullableNumber(tax.rate())) + ','
                + prop("type", tax.type()) + ','
                + rawProp("digitalOnly", String.valueOf(tax.digitalOnly()))
                + "}";
    }

    private static String toJsonShippingPolicies() {
        List<String> result = new ArrayList<>();
        for (ShippingDefinition shipping : SHIPPING_POLICIES) {
            result.add(toJson(shipping));
        }
        return jsonArray(result);
    }

    private static String toJson(ShippingDefinition shipping) {
        return "{"
                + prop("id", shipping.id()) + ','
                + prop("name", shipping.name()) + ','
                + rawProp("cost", number(shipping.cost())) + ','
                + rawProp("threshold", nullableNumber(shipping.threshold()))
                + "}";
    }

    private static String toJsonCheckout(CheckoutResult checkout) {
        List<String> lines = new ArrayList<>();
        for (CheckoutLine line : checkout.lines()) {
            lines.add("{"
                    + prop("productId", line.product().id()) + ','
                    + prop("productName", line.product().name()) + ','
                    + rawProp("quantity", String.valueOf(line.quantity())) + ','
                    + rawProp("basePrice", number(line.basePrice())) + ','
                    + rawProp("priceWithPromo", number(line.priceWithPromotion())) + ','
                    + rawProp("discount", number(line.discount()))
                    + "}");
        }

        return "{"
                + rawProp("itemBreakdown", jsonArray(lines)) + ','
                + rawProp("itemsTotal", number(checkout.itemsTotal())) + ','
                + rawProp("subtotal", number(checkout.subtotal())) + ','
                + rawProp("discountAmount", number(checkout.discountAmount())) + ','
                + prop("promotion", checkout.promotion().name()) + ','
                + prop("promotionId", checkout.promotion().id()) + ','
                + rawProp("appliedPromotion", toJson(checkout.promotion())) + ','
                + rawProp("taxAmount", number(checkout.taxAmount())) + ','
                + prop("taxPolicy", checkout.tax().name()) + ','
                + prop("taxPolicyId", checkout.tax().id()) + ','
                + rawProp("shippingCost", number(checkout.shippingCost())) + ','
                + prop("shippingPolicy", checkout.shipping().name()) + ','
                + prop("shippingPolicyId", checkout.shipping().id()) + ','
                + rawProp("requiresShipping", String.valueOf(checkout.requiresShipping())) + ','
                + rawProp("total", number(checkout.total()))
                + "}";
    }

    private static String prop(String name, String value) {
        return quote(name) + ':' + quote(value);
    }

    private static String rawProp(String name, String rawValue) {
        return quote(name) + ':' + rawValue;
    }

    private static String jsonArray(List<String> jsonItems) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < jsonItems.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(jsonItems.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String nullableString(String value) {
        return value == null ? "null" : quote(value);
    }

    private static String nullableNumber(Double value) {
        return value == null ? "null" : number(value);
    }

    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        double rounded = roundMoney(value);
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.valueOf(rounded);
    }

    private static String quote(String value) {
        return "\"" + escapeJson(value == null ? "" : value) + "\"";
    }

    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String decodePathSegment(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String guessMimeType(Path file) {
        String filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".html")) return "text/html; charset=utf-8";
        if (filename.endsWith(".css")) return "text/css; charset=utf-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (filename.endsWith(".json")) return "application/json; charset=utf-8";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ApiProduct(
            String id,
            String name,
            String description,
            double price,
            int quantity,
            String category,
            String type,
            String image
    ) {
        String stockStatus() {
            if (quantity == 0) return "OUT_OF_STOCK";
            if (quantity <= 10) return "LOW";
            return "IN_STOCK";
        }
    }

    private record PromotionDefinition(String id, String name, String type, Double value) {
        double valueOrZero() {
            return value == null ? 0.0 : value;
        }
    }

    private record TaxDefinition(String id, String name, Double rate, String type, boolean digitalOnly) {
        double rateOrZero() {
            return rate == null ? 0.0 : rate;
        }
    }

    private record ShippingDefinition(String id, String name, double cost, Double threshold) {
    }

    private record CheckoutItem(ApiProduct product, int quantity) {
    }

    private record CheckoutLine(
            ApiProduct product,
            int quantity,
            double basePrice,
            double priceWithPromotion,
            double discount
    ) {
    }

    private record CheckoutResult(
            List<CheckoutLine> lines,
            double itemsTotal,
            double subtotal,
            double discountAmount,
            PromotionDefinition promotion,
            double taxAmount,
            TaxDefinition tax,
            double shippingCost,
            ShippingDefinition shipping,
            double total,
            boolean requiresShipping
    ) {
    }

    private static final class BadRequestException extends RuntimeException {
        BadRequestException(String message) {
            super(message);
        }
    }

    private static final class JsonParser {
        private final String input;
        private int position;

        private JsonParser(String input) {
            this.input = input;
        }

        static Object parse(String input) {
            JsonParser parser = new JsonParser(input);
            Object value = parser.parseValue();
            parser.skipWhitespace();
            if (!parser.isAtEnd()) {
                throw new BadRequestException("Invalid JSON: trailing characters");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw new BadRequestException("Invalid JSON: unexpected end of input");
            }

            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield parseNumber();
                    }
                    throw new BadRequestException("Invalid JSON value at position " + position);
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (tryConsume('}')) {
                return object;
            }

            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new BadRequestException("Invalid JSON object key at position " + position);
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();

                if (tryConsume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (tryConsume(']')) {
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();

                if (tryConsume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isAtEnd()) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (isAtEnd()) {
                        throw new BadRequestException("Invalid JSON escape at end of input");
                    }
                    char escaped = next();
                    switch (escaped) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> sb.append(parseUnicodeEscape());
                        default -> throw new BadRequestException("Invalid JSON escape: \\" + escaped);
                    }
                } else {
                    if (c < 0x20) {
                        throw new BadRequestException("Invalid control character in JSON string");
                    }
                    sb.append(c);
                }
            }
            throw new BadRequestException("Invalid JSON string: missing closing quote");
        }

        private char parseUnicodeEscape() {
            if (position + 4 > input.length()) {
                throw new BadRequestException("Invalid unicode escape");
            }
            String hex = input.substring(position, position + 4);
            try {
                position += 4;
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid unicode escape: " + hex);
            }
        }

        private Number parseNumber() {
            int start = position;
            if (peek() == '-') {
                position++;
            }
            consumeDigits();
            if (!isAtEnd() && peek() == '.') {
                position++;
                consumeDigits();
            }
            if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
                position++;
                if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                    position++;
                }
                consumeDigits();
            }

            String raw = input.substring(start, position);
            try {
                return Double.valueOf(raw);
            } catch (NumberFormatException e) {
                throw new BadRequestException("Invalid number: " + raw);
            }
        }

        private void consumeDigits() {
            int start = position;
            while (!isAtEnd() && Character.isDigit(peek())) {
                position++;
            }
            if (start == position) {
                throw new BadRequestException("Invalid JSON number at position " + position);
            }
        }

        private Object parseLiteral(String literal, Object value) {
            if (!input.startsWith(literal, position)) {
                throw new BadRequestException("Invalid JSON literal at position " + position);
            }
            position += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(peek())) {
                position++;
            }
        }

        private void expect(char expected) {
            if (isAtEnd() || next() != expected) {
                throw new BadRequestException("Expected '" + expected + "' at position " + position);
            }
        }

        private boolean tryConsume(char expected) {
            if (!isAtEnd() && peek() == expected) {
                position++;
                return true;
            }
            return false;
        }

        private char peek() {
            return input.charAt(position);
        }

        private char next() {
            return input.charAt(position++);
        }

        private boolean isAtEnd() {
            return position >= input.length();
        }
    }
}
