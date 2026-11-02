#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_classes=$(mktemp -d)
trap 'rm -rf "$test_classes"' EXIT
javac -d "$test_classes" app/src/main/java/com/readinglog/storytel/LogRules.java app/src/test/java/com/readinglog/storytel/LogRulesTest.java
java -cp "$test_classes" com.readinglog.storytel.LogRulesTest
