#!/usr/bin/env bash
# Compile the examples with javac only (JDK 17+, no Maven or Gradle).
#   ./build.sh            compile into out/
#   ./build.sh run NAME [ARGS...]   run quickstart | sql | app
# Classpath: SDK_JAR (dev.loams:loams) plus protobuf-java, both fetched into .lib/ from
# Maven Central. SDK_JAR may point to a local 0.1.2 jar instead.
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
  fetch dev/loams/loams/0.1.2/loams-0.1.2.jar || { echo "Could not download dev.loams:loams:0.1.2 from Maven Central; check network access or set SDK_JAR" >&2; exit 1; }
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
  exec java -cp "out:$cp" "$main" "${@:3}"
fi
