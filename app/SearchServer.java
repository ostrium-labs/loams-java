package examples;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A small HTTP search service on the JDK's built-in server. {@code GET /search?q=refund&tenant=a}
 * runs a hybrid (vector + BM25 + filter) query and returns Loams' JSON answer as is.
 */
public final class SearchServer {
    static final int DIM = 8;

    public static void main(String[] args) throws IOException {
        var loams = new Rest("examples-java");
        System.out.println("connected to " + loams.describe());
        loams.ensureNamespace();
        loams.recreateCollection("articles", DIM);
        String[][] docs = {
            {"refund policy for damaged items", "a"},
            {"shipping times and carriers", "a"},
            {"refund window is thirty days", "b"},
            {"how to reset your password", "a"},
            {"track an order in transit", "b"},
        };
        var ops = new StringBuilder();
        for (int i = 0; i < docs.length; i++) {
            ops.append(i == 0 ? "" : ",").append(Rest.upsert(i + 1, docs[i][0], docs[i][1], Rest.embed(docs[i][0], DIM)));
        }
        loams.post("/collections/articles/documents", "{\"ops\": [" + ops + "]}");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8088"));
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/search", ex -> handle(loams, ex));
        server.createContext("/healthz", ex -> reply(ex, 200, "{\"ok\":true}"));
        server.start();
        System.out.println("listening on http://127.0.0.1:" + port + "/search?q=refund&tenant=a");
    }

    static void handle(Rest loams, HttpExchange ex) throws IOException {
        Map<String, String> params = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String kv : raw.split("&")) {
                String[] p = kv.split("=", 2);
                params.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                        p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
            }
        }
        String q = params.getOrDefault("q", "");
        if (q.isBlank()) {
            reply(ex, 400, "{\"error\":\"missing q\"}");
            return;
        }
        String tenant = params.get("tenant");
        String filter = tenant == null ? "" : "\"filter\": {\"term\": {\"tenant\": " + Rest.q(tenant) + "}},";
        try {
            var res = loams.post("/query", """
                {"from": "collections.articles",
                 "retrieve": [
                   {"vector": {"field": "embedding", "query": %s, "k": 10}},
                   {"text": {"field": "body", "query": %s, "k": 10}}],
                 %s
                 "fuse": {"method": "rrf", "k": 60}, "limit": 5}
                """.formatted(Rest.vec(Rest.embed(q, DIM)), Rest.q(q), filter));
            reply(ex, 200, toJson(res));
        } catch (IllegalStateException e) {
            reply(ex, 502, "{\"error\":" + Rest.q(String.valueOf(e.getMessage())) + "}");
        }
    }

    @SuppressWarnings("unchecked")
    static String toJson(Object o) {
        if (o instanceof Map<?, ?> m) {
            var sb = new StringBuilder("{");
            boolean first = true;
            for (var e : ((Map<String, Object>) m).entrySet()) {
                sb.append(first ? "" : ",").append(Rest.q(e.getKey())).append(':').append(toJson(e.getValue()));
                first = false;
            }
            return sb.append('}').toString();
        }
        if (o instanceof Iterable<?> l) {
            var sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : l) {
                sb.append(first ? "" : ",").append(toJson(x));
                first = false;
            }
            return sb.append(']').toString();
        }
        if (o instanceof String s) return Rest.q(s);
        return String.valueOf(o);
    }

    static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("content-type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
