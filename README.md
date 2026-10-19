# Loams Java examples

[Loams](https://loams.dev) is an open-source engine for your data in your own bucket: collections with vector, full-text (BM25) and filtered hybrid search, SQL over the same data, and live queries.
These examples use the Java SDK (`dev.loams:loams`, 0.1.0, preview) against a local engine.

| Example | What it shows |
|---|---|
| [quickstart](quickstart) | connect, create a collection, upsert documents, hybrid search (vector + BM25 + filter) |
| [sql](sql) | SQL over a collection: SELECT, GROUP BY, and `rrf(vector_search(...), text_search(...))` |
| [app](app) | a small HTTP search service on the JDK's built-in server, no framework |

## Note on SDK coverage

The 0.1.0 Java SDK covers the Connect surface (instance, live, approvals, ...) and has no module for collections, search or SQL yet. The examples connect and feature-detect through the SDK `Client`, then call the documented native REST API (`/v1/namespaces/{ns}/...`, see the [engine quickstart](https://github.com/ostrium-labs/loams/blob/main/docs/engine-quickstart.md)) with the JDK HTTP client. The helper is [common/Rest.java](common/Rest.java). Typed collection calls will replace it when the SDK grows them.

## Run

Needs JDK 17 or newer and `curl`. Start an engine:

```sh
cargo run -p loams -- dev          # from a checkout of github.com/ostrium-labs/loams
# or use the compose stack from the release docs
```

The examples read `LOAMS_ENDPOINT` (default `http://127.0.0.1:18080`; point it at your engine, for example `http://127.0.0.1:8080`) and only touch the namespace `examples-java`.

```sh
./build.sh                # compile all examples into out/
./build.sh run quickstart
./build.sh run sql
./build.sh run app        # then: curl 'localhost:8088/search?q=refund&tenant=a'
```

`build.sh` fetches the SDK jar and protobuf-java from Maven Central into `.lib/`. If `dev.loams:loams:0.1.0` is not published there yet, build it from the monorepo and point at it:

```sh
git clone https://github.com/ostrium-labs/loams && loams/sdks/java/build.sh package /path/to/out
SDK_JAR=/path/to/out/loams-0.1.0.jar ./build.sh run quickstart
```

Maven: `<dependency><groupId>dev.loams</groupId><artifactId>loams</artifactId><version>0.1.0</version></dependency>`; Gradle: `implementation("dev.loams:loams:0.1.0")`.

## Links

- Docs: <https://loams.dev/docs>
- Release notes and preview status: [docs/release/preview.md](https://github.com/ostrium-labs/loams/blob/main/docs/release/preview.md)
- The SDK: [sdks/java](https://github.com/ostrium-labs/loams/tree/main/sdks/java)

Apache-2.0.
