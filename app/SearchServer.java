package examples;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.loams.Client;
import dev.loams.Filter;
import dev.loams.Fusion;
import dev.loams.Search;
import dev.loams.Values;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small HTTP search service; its Loams requests use the SDK's typed app modules. */
public final class SearchServer {
    private SearchServer() {}

    public static void main(String[] args) throws IOException {
        Client loams = Demo.client();
        Demo.seed(loams, "service_articles");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8088"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/search", exchange -> handle(loams, exchange));
        server.createContext("/healthz", exchange -> reply(exchange, 200, "{\"ok\":true}"));
        server.start();
        System.out.println("listening on http://127.0.0.1:" + port + "/search?q=refund&tenant=a");
    }

    private static void handle(Client loams, HttpExchange exchange) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String kv : raw.split("&")) {
                String[] pair = kv.split("=", 2);
                params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
        }
        String query = params.getOrDefault("q", "");
        if (query.isBlank()) {
            reply(exchange, 400, "{\"error\":\"missing q\"}");
            return;
        }
        try {
            var request = Search.on("service_articles").text(query, "body")
                    .vector("embedding", Demo.embed(query)).fuse(Fusion.rrf(60))
                    .limit(Integer.parseInt(params.getOrDefault("limit", "5")));
            if (params.containsKey("tenant")) request.filter(Filter.eq("tenant", params.get("tenant")));
            var answer = loams.search().query(request);
            List<Object> hits = new ArrayList<>();
            for (var hit : answer.getHitsList()) {
                hits.add(Map.of("pk", Values.idText(hit.getPk()), "score", hit.getScore(),
                        "source", Values.toPlain(hit.getSource())));
            }
            reply(exchange, 200, json(Map.of("hits", hits)));
        } catch (RuntimeException e) {
            reply(exchange, 502, json(Map.of("error", String.valueOf(e.getMessage()))));
        }
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> fields = new ArrayList<>();
            map.forEach((key, item) -> fields.add(quote(String.valueOf(key)) + ":" + json(item)));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable<?> items) {
            List<String> values = new ArrayList<>();
            items.forEach(item -> values.add(json(item)));
            return "[" + String.join(",", values) + "]";
        }
        return quote(value.toString());
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("content-type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
    }
}
