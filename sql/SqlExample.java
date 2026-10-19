package examples;

/** SQL over a collection, including rrf(vector_search(...), text_search(...)). */
public final class SqlExample {
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
        };
        var ops = new StringBuilder();
        for (int i = 0; i < docs.length; i++) {
            ops.append(i == 0 ? "" : ",").append(Rest.upsert(i + 1, docs[i][0], docs[i][1], Rest.embed(docs[i][0], DIM)));
        }
        loams.post("/collections/kb/documents", "{\"ops\": [" + ops + "]}");

        run(loams, "plain SELECT", "SELECT _id, body, tenant FROM kb ORDER BY _id");
        run(loams, "aggregate", "SELECT tenant, count(*) AS n FROM kb GROUP BY tenant ORDER BY tenant");
        run(loams, "hybrid (rrf)",
                "SELECT _id, body, _score FROM rrf(vector_search('kb', %s, 'embedding', 10), text_search('kb', 'refund', 'body', 10)) LIMIT 3"
                        .formatted(Rest.vec(Rest.embed("refund", DIM))));
    }

    static void run(Rest loams, String label, String sql) {
        System.out.println("-- " + label + ": " + sql);
        var res = loams.post("/sql", "{\"query\": " + Rest.q(sql) + "}");
        System.out.println(res);
    }
}
