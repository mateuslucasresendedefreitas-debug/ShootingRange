#!/usr/bin/env bash
# Headless gameplay simulation: a bot plays every operation with every faction
# on the JVM (Android classes shadowed by tests/headless/stubs). Catches
# runtime crashes in the whole simulation layer without a device.
#
# Requires: JDK 17+, python3 (to fetch tools/build classes on first run).
set -euo pipefail
cd "$(dirname "$0")/.."

TOOLS="${ANDRO_TOOLS:-tools}"
CLASSES="build_nosdk/classes"

if [ ! -d "$CLASSES" ] || [ ! -f "$TOOLS/android-all.jar" ]; then
    echo "[sim] building game classes first..."
    ANDRO_TOOLS="$TOOLS" python3 scripts/build_apk.py
fi

OUT="build_nosdk/sim"
mkdir -p "$OUT/stubs" "$OUT/harness"

echo "[sim] compiling stubs + harness"
find tests/headless/stubs -name '*.java' > "$OUT/stub_sources.txt"
javac -nowarn -d "$OUT/stubs" -cp "$TOOLS/android-all.jar" @"$OUT/stub_sources.txt"
javac -nowarn -d "$OUT/harness" \
    -cp "$OUT/stubs:$CLASSES:$TOOLS/android-all.jar" \
    tests/headless/harness/HeadlessSim.java

echo "[sim] running"
java -cp "$OUT/stubs:$OUT/harness:$CLASSES:$TOOLS/android-all.jar" HeadlessSim
