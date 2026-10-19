# app

A search service on `com.sun.net.httpserver`. On start it creates collection `articles` in `examples-java` and loads five documents; then:

```sh
../build.sh run app
curl 'localhost:8088/search?q=refund&tenant=a'   # PORT overrides 8088
```
