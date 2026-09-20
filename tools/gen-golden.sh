#!/usr/bin/env bash
# Regenerates the committed golden SVG snapshots of the reference renderer.
#
# The goldens are Java snapshots of this implementation (text metrics are pure
# Java). The synthetic fixtures under src/test/resources/reference drive
# GoldenRenderTest and the XMLSpy conformance fixture under
# tools/xmlspy drives ConformanceRenderTest.
#
# Usage: tools/gen-golden.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

mvn -o test -Dtest=GoldenRenderTest,ConformanceRenderTest -Dgolden.update=true

echo "SVG goldens regenerated in src/test/resources/golden"
