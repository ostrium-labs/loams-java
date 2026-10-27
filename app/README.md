# app

A search service on `com.sun.net.httpserver`. On start it creates/reuses `service_articles` in the configured example namespace and seeds five documents through the SDK. Its Loams hybrid search uses `client.search()` and `Search`/`Filter`/`Fusion` builders; only the inbound app endpoint uses HTTP:

```sh
../build.sh run app
curl 'localhost:8088/search?q=refund&tenant=a'   # PORT overrides 8088
```
