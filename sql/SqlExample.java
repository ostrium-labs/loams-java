package examples;

import dev.loams.Client;
import dev.loams.SqlModule;
import java.util.List;

/** SQL through the Java SDK module, over the same documents used by the other examples. */
public final class SqlExample {
    private SqlExample() {}

    public static void main(String[] args) {
        Client loams = Demo.client();
        Demo.seed(loams, "articles");

        run(loams, "group by", "SELECT tenant, count(*) AS n FROM articles GROUP BY tenant ORDER BY tenant", List.of());
        run(loams, "parameterized select", "SELECT body, year FROM articles WHERE year >= $1 ORDER BY year", List.of(2024));
        run(loams, "hybrid retrieval in SQL",
                "SELECT _id, body, _score FROM rrf(vector_search('articles', $1, 'embedding', 10), "
                        + "text_search('articles', 'refund', 'body', 10)) LIMIT 3",
                List.of(Demo.embed("refund policy")));
    }

    private static void run(Client loams, String label, String sql, List<?> params) {
        System.out.println("-- " + label);
        for (var row : SqlModule.records(loams.sql().query(sql, params))) {
            System.out.println(row);
        }
    }
}
