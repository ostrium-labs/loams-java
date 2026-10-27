package examples;

import dev.loams.AlreadyExistsException;
import dev.loams.Client;
import dev.loams.Doc;
import dev.loams.Options;
import dev.loams.Schema;
import dev.loams.gen.loams.collection.v1.SearchResponse;
import dev.loams.gen.loams.instance.v1.GetInstanceRequest;
import java.util.List;
import java.util.Map;

/** Shared setup for the examples; all engine calls go through the Java SDK modules. */
public final class Demo {
    public static final String NAMESPACE = System.getenv().getOrDefault("LOAMS_NAMESPACE", "examples-java");
    public static final int DIM = 8;

    private Demo() {}

    public static Client client() {
        Client client = Client.of(Options.fromEnv().namespace(NAMESPACE).sessionConsistency(true).build());
        var info = client.instance().getInstance(GetInstanceRequest.getDefaultInstance());
        System.out.println("connected to " + info.getName() + " " + info.getServerVersion()
                + "; APIs " + info.getApiVersionsList());
        System.out.println("SDK-served modules: " + client.system().served());
        try {
            client.namespaces().create(NAMESPACE);
        } catch (AlreadyExistsException ignored) {
            // Reuse the dedicated example namespace.
        }
        return client;
    }

    public static void seed(Client client, String collection) {
        try {
            client.collections().create(collection, new Schema().text("body").keyword("tenant")
                    .i64("year").vector("embedding", DIM));
        } catch (AlreadyExistsException ignored) {
            // The example's stable schema is safe to reuse.
        }
        client.collection(collection).upsert(List.of(
                doc(1, "refund policy for damaged items", "a", 2024),
                doc(2, "shipping times and carriers", "a", 2025),
                doc(3, "refund window is thirty days", "b", 2023),
                doc(4, "how to reset your password", "a", 2022),
                doc(5, "track an order in transit", "b", 2025)));
    }

    private static Doc doc(long id, String body, String tenant, long year) {
        return new Doc(id, Map.of("body", body, "tenant", tenant, "year", year),
                Map.of("embedding", embed(body)));
    }

    public static float[] embed(String text) {
        float[] vector = new float[DIM];
        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (!word.isEmpty()) vector[Math.floorMod(word.hashCode(), DIM)] += 1f;
        }
        double norm = 0;
        for (float value : vector) norm += value * value;
        norm = Math.sqrt(norm);
        if (norm == 0) vector[0] = 1f;
        else for (int i = 0; i < vector.length; i++) vector[i] /= (float) norm;
        return vector;
    }

    public static void printHits(SearchResponse answer) {
        for (var hit : answer.getHitsList()) {
            System.out.printf("  %s %.4f  %s%n", dev.loams.Values.idText(hit.getPk()), hit.getScore(),
                    hit.getSource().getFieldsOrThrow("body").getStringValue());
        }
    }
}
