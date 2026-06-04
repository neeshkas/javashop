package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server that exposes product data over REST without external dependencies.
 * Endpoints:
 *  - GET /api/products           -> returns all products
 *  - GET /api/products/{id}      -> returns product by id or 404
 *  - /static/*                   -> serves files from ./static folder (images)
 *
 * Data is kept in-memory but lives on the backend side, replacing the old frontend mocks.
 */
public class ProductApiServer {

    private static final int PORT = 8086;
    private static final List<ApiProduct> PRODUCTS = seedProducts();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/products", ProductApiServer::handleProducts);
        server.createContext("/static", ProductApiServer::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Product API server started on http://localhost:" + PORT);
        server.start();
    }

    // ---------- HTTP HANDLERS ----------

    private static void handleProducts(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmptyOk(exchange);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed");
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // /api/products
        if ("/api/products".equals(path)) {
            sendJson(exchange, 200, toJsonArray(PRODUCTS));
            return;
        }

        // /api/products/{id}
        String[] parts = path.split("/");
        if (parts.length == 4) {
            String id = parts[3];
            ApiProduct product = PRODUCTS.stream()
                    .filter(p -> p.id.equals(id))
                    .findFirst()
                    .orElse(null);
            if (product == null) {
                send(exchange, 404, "Not Found");
            } else {
                sendJson(exchange, 200, toJson(product));
            }
            return;
        }

        send(exchange, 404, "Not Found");
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        addCors(exchange);

        if (isOptions(exchange)) {
            sendEmptyOk(exchange);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath().replaceFirst("/static/?", "");
        if (path.isEmpty()) {
            send(exchange, 404, "Not Found");
            return;
        }

        Path file = Path.of("static", path);
        if (!Files.exists(file) || Files.isDirectory(file)) {
            send(exchange, 404, "Not Found");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", guessMimeType(file));
        headers.set("Content-Length", String.valueOf(bytes.length));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- DATA ----------

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

        return Collections.unmodifiableList(list);
    }

    // ---------- HELPERS ----------

    private static boolean isOptions(HttpExchange exchange) {
        return "OPTIONS".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private static void addCors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendEmptyOk(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJsonArray(List<ApiProduct> products) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < products.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(toJson(products.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String toJson(ApiProduct p) {
        String categoryValue = p.category == null ? "null" : quote(p.category);
        return new StringBuilder()
                .append('{')
                .append("\"id\":").append(quote(p.id)).append(',')
                .append("\"name\":").append(quote(p.name)).append(',')
                .append("\"description\":").append(quote(p.description)).append(',')
                .append("\"price\":").append(p.price).append(',')
                .append("\"quantity\":").append(p.quantity).append(',')
                .append("\"category\":").append(categoryValue).append(',')
                .append("\"stockStatus\":").append(quote(p.stockStatus())).append(',')
                .append("\"type\":").append(quote(p.type)).append(',')
                .append("\"image\":").append(quote(p.image))
                .append('}')
                .toString();
    }

    private static String quote(String s) {
        return "\"" + escapeJson(s) + "\"";
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

    private static String guessMimeType(Path file) {
        String filename = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    // ---------- DATA STRUCT ----------

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
}
