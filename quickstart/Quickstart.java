package examples;

import dev.loams.Client;
import dev.loams.Filter;
import dev.loams.Fusion;
import dev.loams.ProducerRecord;
import dev.loams.Search;
import dev.loams.SqlModule;
import dev.loams.UnimplementedException;
import dev.loams.gen.loams.collection.v1.SearchResponse;
import dev.loams.gen.loams.live.v1.ListAppsRequest;
import java.util.List;
import java.util.Map;

/** SDK tour: namespace, collection, documents, text/vector/hybrid search, graph, SQL and streams. */
public final class Quickstart {
    private Quickstart() {}

    public static void main(String[] args) {
        Client loams = Demo.client();
        Demo.seed(loams, "articles");

        SearchResponse text = loams.search().query(Search.on("articles").text("refund", "body")
                .filter(Filter.eq("tenant", "a")).limit(3));
        System.out.println("text search (tenant a):");
        Demo.printHits(text);

        System.out.println("vector search:");
        Demo.printHits(loams.vector().search("articles", Demo.embed("refund policy"), "embedding", 3, null));

        SearchResponse hybrid = loams.search().query(Search.on("articles")
                .text("refund", "body").vector("embedding", Demo.embed("refund policy"))
                .fuse(Fusion.rrf(60)).filter(Filter.eq("tenant", "a")).limit(3));
        System.out.println("hybrid search (vector + BM25 + filter, RRF):");
        Demo.printHits(hybrid);

        try {
            loams.graph().create("examples_graph");
            loams.graph().query("examples_graph", "INSERT (:Article {id: $id})", Map.of("id", 1L), false);
            var result = loams.graph().query("examples_graph", "MATCH (a:Article) RETURN a.id", null, true);
            System.out.println("experimental graph returned " + result.getRows().getRowsCount() + " row(s)");
        } catch (UnimplementedException unavailable) {
            System.out.println("graph is not enabled in this engine build");
        }

        var sql = loams.sql().query("SELECT tenant, count(*) AS n FROM articles GROUP BY tenant ORDER BY tenant", List.of());
        System.out.println("SQL rows: " + SqlModule.records(sql));

        loams.streams().create("example_events", 1);
        var write = loams.streams().produce("example_events", 0,
                List.of(ProducerRecord.of("article-1", Map.of("kind", "indexed", "id", 1))),
                "java-example", 1L);
        try (var subscription = loams.streams().subscribe("example_events", 0, write.getBaseOffset(), 1000)) {
            if (subscription.hasNext()) {
                var event = subscription.next();
                System.out.println("stream event at offset " + event.offset() + ": " + event.text());
            }
        }

        // Optional and read-only: the Live admin service needs a server that serves it and
        // credentials with directory access. It never creates or changes an app.
        if (List.of(args).contains("--live-admin")) {
            var absent = loams.system().guard("live.admin");
            if (absent != null) throw absent;
            var apps = loams.liveAdmin().listApps(ListAppsRequest.getDefaultInstance());
            System.out.println("Live apps visible to this credential: " + apps.getAppsCount());
        }
    }
}
