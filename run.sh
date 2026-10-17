#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mode="${1:-demo}"
case "$mode" in demo|interactive|test) ;; *) echo 'Usage: bash run.sh [demo|interactive|test]'; exit 1 ;; esac
javac_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
if [[ "$mode" == test ]]; then find tests -name '*.java' >> build/sources.txt; fi
"$javac_cmd" --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/sources.txt
if [[ "$mode" == test ]]; then
    "$java_cmd" -cp build/classes assignment4.OrderFacadeTest
elif [[ "$mode" == interactive ]]; then
    "$java_cmd" -cp build/classes assignment4.Main --interactive
else
    "$java_cmd" -cp build/classes assignment4.Main
fi
