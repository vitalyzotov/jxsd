#!/usr/bin/env python3
"""Compares our reference renderer against XMLSpy diagrams for the conformance
fixtures.

For every global element and named complex type in the fixture schemas it:
  1. renders the component with the shaded CLI (`-r <name> -e 1 -d`),
  2. pairs the result with a page under the XMLSpy output directory by matching
     the component name against the page's text runs,
  3. writes an HTML side-by-side report and a machine summary of geometry and
     label differences.

The XMLSpy SVGs are proprietary and stay local; only this script and the
fixtures are committed. Generate the XMLSpy side with both schemas into
tools/xmlspy/out/ before running.

Usage:
    tools/xmlspy/compare.py [--xmlspy-dir tools/xmlspy/out]
                            [--jar target/jxsd-1.0-SNAPSHOT.jar]
                            [--report tools/xmlspy/report]
"""
from __future__ import annotations

import argparse
import html
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent


def local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def schema_roots(xsd: Path) -> list[tuple[str, str]]:
    """Returns (name, kind) for the global elements and complex types."""
    found: list[tuple[str, str]] = []
    seen: set[str] = set()
    root = ET.parse(xsd).getroot()
    for child in root:
        kind = local(child.tag)
        if kind not in ("element", "complexType"):
            continue
        name = child.get("name")
        if not name:
            continue
        if name in seen:
            continue
        seen.add(name)
        found.append((name, kind))
    return found


def render(cli_jar: Path, xsd: Path, name: str, out: Path) -> tuple[bool, str]:
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "java", "-jar", str(cli_jar),
        "-r", name, "-e", "1", "-d",
        "-o", str(out), str(xsd),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        return False, (result.stderr or result.stdout).strip()
    return True, ""


def svg_texts(svg: Path) -> list[str]:
    texts: list[str] = []
    try:
        root = ET.parse(svg).getroot()
    except ET.ParseError:
        return texts
    for node in root.iter():
        if local(node.tag) == "text" and node.text:
            value = node.text.strip()
            if value:
                texts.append(value)
    return texts


_ANCHOR = re.compile(r'<a name="Link[0-9A-F]+">')
_HEADER = re.compile(r'elementHeader2">([^<]+)')
_IMG = re.compile(r'src="([^"]+\.svg)"')


def load_page_map(xmlspy_dir: Path) -> dict[str, Path]:
    """Maps a component name to its XMLSpy page using the generated HTML report.

    XMLSpy's documentation HTML lists every global component and embeds the
    diagram image right after its ``elementHeader2`` title, which is an exact
    mapping (unlike matching the diagram text runs).
    """
    pages: dict[str, Path] = {}
    for report in sorted(xmlspy_dir.rglob("*.html")):
        text = report.read_text(encoding="utf-8", errors="replace")
        for block in _ANCHOR.split(text)[1:]:
            header = _HEADER.search(block)
            image = _IMG.search(block)
            if not header or not image:
                continue
            name = header.group(1).strip()
            if "/" in name:
                continue  # child-element detail page
            pages.setdefault(name, (report.parent / image.group(1)).resolve())
    return pages


def find_xmlspy_page(pages: dict[Path, list[str]], name: str) -> Path | None:
    """Pairs a component with a page by matching its name against text runs.

    XMLSpy truncates long labels with a trailing "...", so a page matches when
    its text run equals the name or is a prefix of it.
    """
    best: Path | None = None
    for page, texts in pages.items():
        for text in texts:
            # strip a namespace prefix such as "ns3:" or "cnf:"
            tail = text.rsplit(":", 1)[-1]
            if tail == name:
                return page
            if tail.endswith("...") and name.startswith(tail[:-3]):
                best = best or page
            elif name in tail:
                best = best or page
    return best


def summarize(svg: Path) -> dict:
    root = ET.parse(svg).getroot()
    counts: Counter[str] = Counter()
    labels: set[str] = set()
    for node in root.iter():
        kind = local(node.tag)
        if kind == "text":
            counts["text"] += 1
            if node.text and node.text.strip():
                labels.add(re.sub(r"\s+", " ", node.text.strip()))
        elif kind == "rect":
            counts["rect"] += 1
        elif kind == "path":
            counts["path"] += 1
    return {
        "width": root.get("width"),
        "height": root.get("height"),
        "counts": counts,
        "labels": labels,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--main", type=Path, default=HERE / "xmlspy.conformance.v1.xsd")
    parser.add_argument("--shared", type=Path, default=HERE / "xmlspy.shared.v1.xsd")
    parser.add_argument("--xmlspy-dir", type=Path, default=HERE / "out")
    parser.add_argument("--jar", type=Path, default=ROOT / "target/jxsd-1.0-SNAPSHOT.jar")
    parser.add_argument("--report", type=Path, default=HERE / "report")
    args = parser.parse_args()

    if not args.jar.exists():
        print(f"CLI jar not found: {args.jar}\nBuild it with: mvn -o package", file=sys.stderr)
        return 2

    roots: list[tuple[str, str, Path]] = []
    for xsd in (args.main, args.shared):
        for name, kind in schema_roots(xsd):
            roots.append((name, kind, xsd))

    page_map: dict[str, Path] = {}
    pages: dict[Path, list[str]] = {}
    if args.xmlspy_dir.is_dir():
        page_map = load_page_map(args.xmlspy_dir)
        for page in sorted(args.xmlspy_dir.rglob("*.svg")):
            pages[page] = svg_texts(page)

    ours_dir = args.report / "ours"
    rows = []
    for name, kind, xsd in roots:
        out = ours_dir / f"{name}.svg"
        ok, error = render(args.jar, xsd, name, out)
        produced = ok and out.exists()
        page = page_map.get(name)
        if page is None and pages:
            page = find_xmlspy_page(pages, name)
        ours_summary = summarize(out) if produced else None
        xspy_summary = summarize(page) if page else None
        rows.append((name, kind, out if produced else None, error, page, ours_summary, xspy_summary))

    if not pages:
        print(f"No XMLSpy pages under {args.xmlspy_dir}; rendering our side only.")
    else:
        print(f"XMLSpy report map: {len(page_map)} components")

    args.report.mkdir(parents=True, exist_ok=True)
    written = write_html(args.report / "index.html", rows)
    report_lines = write_summary(rows)

    print(f"Rendered {sum(1 for r in rows if r[2])} of {len(rows)} components")
    print(f"Paired {sum(1 for r in rows if r[4])} components with XMLSpy pages")
    print(f"Report: {written}")
    print("\n".join(report_lines))
    return 0


def write_html(path: Path, rows) -> Path:
    parts = [
        "<!doctype html><meta charset=\"utf-8\">",
        "<title>XMLSpy conformance</title>",
        "<style>body{font-family:sans-serif}table{border-collapse:collapse}"
        "td,th{border:1px solid #ccc;padding:4px;vertical-align:top}"
        "img{background:#fff;border:1px solid #eee;max-width:640px}</style>",
        "<h1>XMLSpy conformance</h1>",
        "<table><tr><th>component</th><th>ours</th><th>XMLSpy</th><th>notes</th></tr>",
    ]
    for name, kind, ours, error, page, ours_summary, xspy_summary in rows:
        rel_ours = os.path.relpath(ours, path.parent) if ours else None
        rel_page = os.path.relpath(page, path.parent) if page else None
        ours_cell = f'<img src="{html.escape(rel_ours)}">' if rel_ours else f"<em>rejected</em><br><code>{html.escape(error)}</code>"
        xspy_cell = f'<img src="{html.escape(rel_page)}">' if rel_page else "<em>no page</em>"
        notes = ""
        if ours_summary and xspy_summary:
            delta = "".join(
                f"{k}: {xspy_summary['counts'][k]}→{ours_summary['counts'][k]} "
                for k in ("rect", "path", "text")
                if xspy_summary["counts"][k] != ours_summary["counts"][k]
            )
            size = f"{xspy_summary['width']}x{xspy_summary['height']} vs {ours_summary['width']}x{ours_summary['height']}"
            notes = f"{html.escape(size)}<br>{html.escape(delta)}"
        parts.append(
            f"<tr><td>{html.escape(name)}<br><small>{kind}</small></td>"
            f"<td>{ours_cell}</td><td>{xspy_cell}</td><td>{notes}</td></tr>"
        )
    parts.append("</table>")
    path.write_text("\n".join(parts), encoding="utf-8")
    return path


def write_summary(rows) -> list[str]:
    lines = []
    for name, kind, ours, error, page, ours_summary, xspy_summary in rows:
        if not ours:
            lines.append(f"REJECTED  {name} ({kind}): {error.splitlines()[0] if error else ''}")
            continue
        if not xspy_summary:
            lines.append(f"UNPAIRED  {name} ({kind})")
            continue
        counts = " ".join(
            f"{k} xspy={xspy_summary['counts'][k]} ours={ours_summary['counts'][k]}"
            for k in ("rect", "path", "text")
        )
        missing = sorted(xspy_summary["labels"] - ours_summary["labels"])
        extra = sorted(ours_summary["labels"] - xspy_summary["labels"])
        detail = ""
        if missing:
            detail += " missing=" + ",".join(repr(m) for m in missing[:6])
        if extra:
            detail += " extra=" + ",".join(repr(m) for m in extra[:6])
        lines.append(f"{name} ({kind}): {counts}{detail}")
    return lines


if __name__ == "__main__":
    raise SystemExit(main())
