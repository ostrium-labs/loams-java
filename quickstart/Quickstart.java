package examples;

import java.util.Map;

/** Connect, create a collection, upsert documents, run a hybrid search, print the hits. */
public final class Quickstart {
    static final int DIM = 8;

    public static void main(String[] args) {
        var loams = new Rest("examples-java");
        System.out.println("connected to " + loams.describe());

        loams.ensureNamespace();
        loams.recreateCollection("kb", DIM);

        String[][] docs = {
            {"refund policy for damaged items", "a"},
            {"shipping times and carriers", "a"},
            {"refund window is thirty days", "b"},
            {"how to reset your password", "a"},
        };
        var ops = new StringBuilder();
        for (int i = 0; i < docs.length; i++) {
            ops.append(i == 0 ? "" : ",").append(Rest.upsert(i + 1, docs[i][0], docs[i][1], Rest.embed(docs[i][0], DIM)));
        }
        loams.post("/collections/kb/documents", "{\"ops\": [" + ops + "]}");
        System.out.println("upserted " + docs.length + " documents");

        String query = "refund";
        var result = loams.post("/query", """
            {"from": "collections.kb",
             "retrieve": [
               {"vector": {"field": "embedding", "query": %s, "k": 10}},
               {"text": {"field": "body", "query": %s, "k": 10}}],
             "filter": {"term": {"tenant": "a"}},
             "fuse": {"method": "rrf", "k": 60},
             "limit": 3}
            """.formatted(Rest.vec(Rest.embed(query, DIM)), Rest.q(query)));

        System.out.println("hybrid search for \"" + query + "\" (tenant a):");
        for (Object hit : Rest.list(result, "hits")) {
            Map<String, Object> h = Rest.map(hit);
            System.out.printf("  id=%d score=%.4f %s%n", ((Number) h.get("pk")).longValue(), ((Number) h.get("score")).doubleValue(), Rest.map(h.get("source")).get("body"));
        }
    }
}
