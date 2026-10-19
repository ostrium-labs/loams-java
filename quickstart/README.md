# quickstart

Connects with the SDK, creates collection `kb` in namespace `examples-java`, upserts four documents (a toy hashed embedding stands in for a model), and runs a hybrid query: a vector retriever and a BM25 text retriever, fused with RRF, filtered to `tenant = "a"`.

```sh
../build.sh run quickstart
```
