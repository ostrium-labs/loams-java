# quickstart

Uses the SDK app modules to create/reuse a namespace and collection, upsert articles, run text/vector/hybrid search, and demonstrate experimental graph, parameterized SQL, and stream produce/subscribe. Instance discovery prints the server's API versions. Search embeddings are deterministic toy hashes, not model output.

```sh
../build.sh run quickstart
../build.sh run quickstart --live-admin  # optional, read-only; requires a served Live admin API
```
