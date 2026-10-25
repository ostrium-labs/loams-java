#!/usr/bin/env bash
# Compile the examples with javac only (JDK 17+, no Maven or Gradle).
#   ./build.sh            compile into out/
#   ./build.sh run NAME   run quickstart | sql | app
# Classpath: SDK_JAR (dev.loams:loams) plus protobuf-java, both fetched into .lib/ from
# Maven Central. If dev.loams:loams is not on Central yet, build it from the monorepo
# (`sdks/java/build.sh package DIR` in ostrium-labs/loams) and set SDK_JAR=DIR/loams-0.1.2.jar.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p .lib out
central=https://repo1.maven.org/maven2
fetch() {
  local target=".lib/$(basename "$1")"
  if [ ! -s "$target" ]; then
    curl -fsSL "$central/$1" -o "$target.tmp" || return 1
    mv "$target.tmp" "$target"
  fi
}
fetch com/google/protobuf/protobuf-java/4.29.3/protobuf-java-4.29.3.jar
if [ -z "${SDK_JAR:-}" ]; then
  fetch dev/loams/loams/0.1.2/loams-0.1.2.jar || { echo "dev.loams:loams:0.1.2 is not on Maven Central yet; set SDK_JAR (see header)" >&2; exit 1; }
  SDK_JAR=.lib/loams-0.1.2.jar
fi
cp="$SDK_JAR:.lib/protobuf-java-4.29.3.jar"
javac --release 17 -nowarn -encoding UTF-8 -cp "$cp" -d out common/*.java quickstart/*.java sql/*.java app/*.java
if [ "${1:-}" = run ]; then
  case "${2:?quickstart|sql|app}" in
    quickstart) main=examples.Quickstart ;;
    sql) main=examples.SqlExample ;;
    app) main=examples.SearchServer ;;
    *) echo "unknown example $2" >&2; exit 2 ;;
  esac
  exec java -cp "out:$cp" "$main"
fi
