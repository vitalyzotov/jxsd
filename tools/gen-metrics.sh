#!/usr/bin/env bash
# Regenerates the bundled Segoe UI text metrics from locally installed faces.
#
# The metrics are committed data: the build, the tests and the runtime never need
# the font. Re-run this deliberately when the reference font changes.
#
# Usage: SEGOE_DIR=/path/to/segoe tools/gen-metrics.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SEGOE_DIR="${SEGOE_DIR:-$HOME/.fonts/segoeUI}"

java tools/GenMetrics.java "Segoe UI" \
    "$SEGOE_DIR/segoeui.ttf:400:normal" \
    "$SEGOE_DIR/seguisb.ttf:600:normal" \
    "$SEGOE_DIR/segoeuii.ttf:400:italic" \
    > src/main/resources/metrics/segoe-ui-metrics.txt

echo "Segoe UI metrics regenerated in src/main/resources/metrics/segoe-ui-metrics.txt"
