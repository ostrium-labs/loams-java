package examples;

import dev.loams.Client;
import dev.loams.internal.Json;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Shared helpers for the examples.
 *
 * <p>These examples demonstrate the native HTTP JSON routes alongside the SDK.
 * The SDK also provides app modules, builders, collection handles, SQL and streams.
 */
public final class Rest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public final String endpoint;
    public final String namespace;
    public final Client client;

    public Rest(String namespace) {
        this.endpoint = System.getenv().getOrDefault("LOAMS_ENDPOINT", "http://127.0.0.1:8080");
        this.namespace = namespace;
        this.client = Client.of(Client.builder().endpoint(endpoint).build());
    }

    /** Ask the instance who it is, through the SDK. */
    public String describe() {
        var info = client.instance().getInstance(
                dev.loams.gen.loams.instance.v1.GetInstanceRequest.getDefaultInstance());
        return info.getName() + " " + info.getServerVersion();
    }

    /** POST a JSON body under /v1/namespaces/{ns}/{path}; returns the parsed JSON object. */
    public Map<String, Object> post(String path, String body) {
        return send("POST", "/v1/namespaces/" + namespace + path, body);
    }

    public Map<String, Object> send(String method, String path, String body) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(endpoint + path))
                    .header("content-type", "application/json");
            builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> res = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                throw new IllegalStateException(method + " " + path + " -> " + res.statusCode() + ": " + res.body());
            }
            return res.body().isBlank() ? Map.of() : Json.parseObject(res.body());
        } catch (IOException e) {
            throw new IllegalStateException("cannot reach " + endpoint + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The namespace exists after this call (creating it twice is fine). */
    public void ensureNamespace() {
        try {
            send("POST", "/v1/namespaces", "{\"namespace\":" + q(namespace) + "}");
        } catch (IllegalStateException alreadyThere) {
            // a conflict means it exists; anything else surfaces on the next call
        }
    }

    /** Create the demo collection from scratch: drop a stale one, then create. */
    public void recreateCollection(String name, int dim) {
        try {
            send("DELETE", "/v1/namespaces/" + namespace + "/collections/" + name, null);
        } catch (IllegalStateException missing) {
            // not there yet
        }
        post("/collections", """
            {"name": %s,
             "schema": {
               "fields": [
                 {"name": "body", "source_path": "body", "kind": {"text": {"analyzer": "standard", "positions": true}}, "indexed": true, "fast": false},
                 {"name": "tenant", "source_path": "tenant", "kind": "keyword", "indexed": true, "fast": true}
               ],
               "vectors": [{"name": "embedding", "dim": %d, "distance": "cosine"}],
               "sparse_vectors": [], "dynamic": "ignore", "max_fields": 1000}}
            """.formatted(q(name), dim));
    }

    /** One upsert op. */
    public static String upsert(int id, String body, String tenant, float[] vector) {
        return "{\"upsert\": {\"id\": %d, \"source\": {\"body\": %s, \"tenant\": %s}, \"vectors\": {\"embedding\": %s}}}"
                .formatted(id, q(body), q(tenant), vec(vector));
    }

    public static String vec(float[] v) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) sb.append(i == 0 ? "" : ",").append(v[i]);
        return sb.append(']').toString();
    }

    /** JSON string literal. */
    public static String q(String s) {
        var sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    /**
     * A toy embedding: hashed bag of words into {@code dim} buckets, L2-normalised. It stands in for
     * a real embedding model so the examples run with no external service.
     */
    public static float[] embed(String text, int dim) {
        float[] v = new float[dim];
        for (String w : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!w.isEmpty()) v[Math.floorMod(w.hashCode(), dim)] += 1f;
        }
        double n = 0;
        for (float x : v) n += x * x;
        n = Math.sqrt(n);
        if (n == 0) v[0] = 1f; else for (int i = 0; i < dim; i++) v[i] /= (float) n;
        return v;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }
}
