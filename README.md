# Loams Java examples

These examples target the published **Loams SDK 0.1.2** release.

[Loams](https://loams.dev) is an open-source engine for your data in your own bucket: collections with vector, full-text (BM25) and filtered hybrid search, SQL over the same data, and live queries.
These examples use the Java SDK (`dev.loams:loams`, 0.1.2, preview) against a local engine.

| Example | What it shows |
|---|---|
| [quickstart](quickstart) | namespaces, collections, documents, text/vector/hybrid search, experimental graph, SQL, and streams |
| [sql](sql) | SDK SQL module, parameters, aggregates, and hybrid search table functions |
| [app](app) | HTTP search service using the SDK's typed hybrid-search module |

## Note on SDK coverage

The examples call the published SDK's namespaces, collections, documents, search, vector, graph, SQL and streams modules. The HTTP server in `app/` is an application-facing example; calls from that app to Loams still go through the SDK. They also show instance discovery and the API versions reported by the engine.

| Module family | Example coverage in SDK 0.1.2 |
|---|---|
| `instance`, `system` | Covered by startup discovery and the served-module catalogue. |
| `namespaces`, `collections`, `documents`, `search`, `vector`, `graph`, `sql`, `streams` | Covered in quickstart/SQL; graph is optional when the server enables the experimental package. |
| `live`, `live.admin`, `tables` | Optional SDK modules; `quickstart --live-admin` performs a read-only app-directory listing when served and authorized. Live watch/tables calls are not exercised. |
| Authentication | Optional client configuration via `LOAMS_API_KEY` or `LOAMS_TOKEN`; no sign-in/provisioning flow is implied. |
| House | Unavailable as a typed module in SDK 0.1.2. |

## Run

Needs JDK 17 or newer and `curl`. Start an engine:

```sh
cargo run -p loams -- dev          # from a checkout of github.com/ostrium-labs/loams
# or use the Compose stack from the release docs
```

The examples read `LOAMS_ENDPOINT` (default `http://127.0.0.1:8080`), optional `LOAMS_API_KEY`/`LOAMS_TOKEN`, and `LOAMS_NAMESPACE` (default `examples-java`). They create or update data in that dedicated namespace; use a disposable namespace.

```sh
./build.sh                # compile all examples into out/
./build.sh run quickstart
./build.sh run sql
./build.sh run app        # then: curl 'localhost:8088/search?q=refund&tenant=a'
```

`build.sh` fetches `dev.loams:loams:0.1.2` and protobuf-java from Maven Central into `.lib/`. To use a local SDK build instead, set `SDK_JAR=/path/to/loams-0.1.2.jar`.

Maven: `<dependency><groupId>dev.loams</groupId><artifactId>loams</artifactId><version>0.1.2</version></dependency>`; Gradle: `implementation("dev.loams:loams:0.1.2")`.

## Links

- Docs: <https://loams.dev/docs>
- Release notes and preview status: [docs/release/preview.md](https://github.com/ostrium-labs/loams/blob/v0.1.2/docs/release/preview.md)
- The SDK: [sdks/java](https://github.com/ostrium-labs/loams/tree/v0.1.2/sdks/java)

Apache-2.0.
