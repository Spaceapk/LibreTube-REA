#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" app/src/main/java/com/github/libretube/rea/LocalRanker.java app/src/test/java/com/github/libretube/rea/LocalRankerChecks.java
java -cp "$classes" com.github.libretube.rea.LocalRankerChecks
