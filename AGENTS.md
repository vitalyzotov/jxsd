# AGENTS.md

Java 21 CLI that generates XSD diagrams as SVG reference pages. Output is guarded by committed
Java SVG snapshots of the reference renderer plus structural CLI tests. There is no README/CI;
this file is the entrypoint.

## Provenance

This is an independent implementation; no source code is copied or translated from other XSD
diagram tools. Do not reintroduce identifiers, comments or string literals from another tool:
names must stay Java-idiomatic and original. The *visual* reference is XMLSpy - its conventions
are recovered from the reference diagrams under `tools/xmlspy/` and encoded in `Metrics`, so new
layout work follows those snapshots rather than any external source.

## Build & test

- Full suite: `mvn test` (offline works: `mvn -o test` after the dependencies are cached)
- Single test: `mvn -Dtest=GoldenRenderTest test`
- Shaded CLI jar: `mvn package` -> `target/jxsd-1.0-SNAPSHOT.jar` (main `org.jxsd.cli.Main`)
- Compiler release is 21; a newer JDK (e.g. 25) is fine.
- Runtime deps: picocli, Apache XmlSchema 2 (`org.apache.ws.xmlschema:xmlschema-core`),
  which parses XSD and resolves includes/imports, and Apache Batik
  (`org.apache.xmlgraphics:batik-transcoder` + `batik-codec`), which rasterizes SVG to PNG.
  PNG export needs one online Maven run to cache Batik; after that `-o` works offline.
  Batik rasterization is pure Java but uses AWT fonts, so tests set `java.awt.headless=true`
  in surefire (PNG assertions are metric/font-independent).
- `mvn verify` additionally runs Checkstyle (`config/checkstyle/checkstyle.xml`, 0
  violations required), SpotBugs (`config/spotbugs/exclude.xml`) and the JaCoCo coverage
  gate (>= 80% line coverage of the whole bundle; report at `target/site/jacoco/index.html`).
  Use online Maven at least once so those plugins and the enforcer are cached.

## Text measurement (pure Java)

- Layout widths come from `SegoeUiMetrics` (`rendering/SegoeUiMetrics.java`), which loads the
  bundled `/metrics/segoe-ui-metrics.txt`: per-character advances in font units (unitsPerEm
  2048) for the three used faces (`400 normal`, `600 normal`, `400 italic`). The file is
  generated from the Segoe UI faces by `tools/GenMetrics.java` / `tools/gen-metrics.sh` and is
  committed; the build, tests and runtime never need the font.
- The SVG lays out glyphs naturally: a text run carries one start `x` and no per-glyph
  coordinates, so substituting the font cannot overlap glyphs. Only the totals feed layout
  (box/panel sizing, documentation wrapping).
- `--text-length` emits `textLength`/`lengthAdjust="spacingAndGlyphs"` on every run, pinning it
  to the computed width so the layout survives a font substitution in the viewer.
- Coordinates are formatted with `SegoeUiMetrics.formatCoordinate` (C `%.8g`, half-even,
  trailing zeros trimmed). Do not "simplify" the metrics file or that formatting.
- `RenderingSupportTest.metricsMatchTheGeneratedTable` guards a few known advances; regenerate
  the table with `tools/gen-metrics.sh` when the reference font changes. No native process or
  external tooling is needed to build or test.

## Golden-output rules

- Tests assert **exact bytes** (length + array equality, no tolerance) against committed
  SVG snapshots in `src/test/resources/golden/*.svg` (conformance pages in
  `src/test/resources/golden/conformance/`).
- The synthetic fixtures in `src/test/resources/reference/*.xsd` are invented, abstract
  schemas chosen so every supported page shape is reachable. `GoldenRenderTest`
  drives `Schema.load -> Diagram.addRoot/expand -> PageRenderer(DEFAULT)`
  and byte-compares each scenario. `SchemaLoadTest` covers the compositors that are not
  renderable (`xs:all`, wildcards, group references).
- `tools/xmlspy/` holds the XMLSpy conformance fixture (`xmlspy.conformance.v1.xsd`,
  `xmlspy.shared.v1.xsd`), the expansion fixture (`xmlspy.expand.v1.xsd`) and `compare.py`,
  which renders every component and pairs it with the XMLSpy diagrams under
  `tools/xmlspy/out/` (local, git-ignored). `ConformanceRenderTest` snapshots our output for
  those fixtures (`expand_*_e2/e3.svg`) and is the guard for the recovered conventions.
- Expansion is recursive. An element child that revealed its own children (`-e >= 2`) is an
  `NestedPage.Expanded`: its node plus either a yellow container titled `prefix:Type` (named
  type, with the type's attributes in a tab) or a plain nested column (inline anonymous type),
  holding the recursively laid-out content group. The expanded element connects to the
  container's attributes and content group (a `CompositeBody`-style branch when attributes are
  present, a straight stub otherwise), and element-root containers reach into the content group
  the same way. `PageShapeResolver.isLeaf` treats an element as a leaf only while it has no children,
  and the flat chain/composite/context/extension paths are kept untouched for `-e <= 1`;
  expansion only activates when a child has children.
- Regenerate snapshots deliberately with
  `mvn -o test -Dtest=GoldenRenderTest,ConformanceRenderTest -Dgolden.update=true`
  (or `tools/gen-golden.sh`). Treat a snapshot diff as a deliberate change, not as a bug fix.
- `CliRenderTest` adds metric-independent structural checks (SVG header, labels, the
  `-d/--documentation` and `-l/--language` toggles and run-to-run determinism).
- Any change to layout, coordinate/float formatting, the metrics resource, SVG whitespace,
  or the parsed schema model breaks the snapshots; update them intentionally.
- Child connectors follow the XMLSpy reference: the branch sits at `groupX + 54`, the child
  at `groupX + 69` (a ~15px horizontal stub), plus a short `expand box -> branch` segment;
  a single child is a straight line from the group's expand box at the same `+69` offset.
  A required child's stub is solid; an optional (`minOccurs = 0`) child is reached by a
  dashed horizontal run (`Connector.dashedHorizontal`, wide markers every 6px).
- Node borders are always solid, matching XMLSpy: optional children
  (`minOccurs = 0`, and prohibited ones) are marked by the hand-drawn dashed
  **connector** instead. Do not reintroduce dashed node borders.
- Stacked siblings use the recovered XMLSpy pitch: `Metrics.SIBLING_ELEMENT_GAP = 12`
  between element rows (`attribute` rows are denser at `SIBLING_ATTRIBUTE_GAP = 7`). A node
  with documentation simply adds the panel height; there is no separate "bottom gap".
- Documentation word-wraps at `Metrics.DOCUMENTATION_WRAP_WIDTH = 115` (the reference
  column is ~111px wide). A source newline starts a paragraph, its leading whitespace is kept
  as a first-line indent, and internal whitespace collapses.
- Bold node labels wider than `Metrics.LABEL_TRUNCATION_WIDTH = 170` are truncated and
  suffixed with `...` (`Labels`), matching the reference.
- Documentation text has **no background rect**: it draws directly on the page (or on the
  yellow extension container). The legacy white panel masked nothing and produced white
  squares inside the extension container.
- The `clipPath` rect spans `x=-10 y=-5` with `canvasWidth + 10` by `canvasHeight + 5`; each
  page then starts with an opaque white backdrop `rect` at the origin of the same size,
  matching the XMLSpy reference.

## Architecture

Pipeline: `Schema.load` -> `Diagram.addRoot` / `expand` -> `PageRenderer`
-> SVG.

- `parsing/`: `Schema` is the facade over an Apache XmlSchema 2 `XmlSchemaCollection`;
  `SchemaReader` parses a source and collects the top-level `SchemaComponent`s (each typed
  by a `ComponentKind`), and
  `SchemaDependencyResolver` is the custom `URIResolver` for http/auth/missing dependencies.
  Every input (the root document and each dependency) is materialised and checked by
  `ExternalContentPolicy`: a DOCTYPE is refused by default (XXE/SSRF hardening) and plain
  HTTP is refused when credentials are configured (the Basic-auth header would travel
  unencrypted). `--insecure` opts out of both.
  `AttributeEnumerator` enumerates attributes (base types + attribute groups) and
  `AnnotationText` extracts `xml:lang` documentation.
- `model/`: `Diagram` is the builder/session holding the options, the `Schema` and the root
  items, and exposes an immutable `DiagramContext(schema, showDocumentation, language)`.
  `DiagramNode` is a sealed tree of `ElementItem` / `TypeItem` / `ModelGroupItem` / `GroupRefItem` /
  `WildcardItem`: an anonymous XSD model group carries a `Compositor` (`sequence`/`choice`/`all`
  only), a group reference is a separate particle and a wildcard a leaf. It is built by
  `DiagramNodeFactory` from the resolved particles/types; each node reveals its own content
  (`DiagramNode.expandContent`) through the traversal primitives of `NodeExpander`, and
  `Diagram.expand(levels)` drives it with a breadth-first pass over the expansion frontier
  (one level per pass, bounded by the tree). A node exposes only a narrow `NodeSource` to the
  renderer. Element type references are a sealed `TypeRef` (named/anonymous), content is an
  explicit `ContentType` and the element's declaration/reference origin is `ElementUse`.
  Depends on `parsing`, never on `rendering`.
- `rendering/`: `PageShapeResolver` picks a `PageShape` strategy and `PageRenderer` renders it
  (chain, composite, context, nested or simple element), emitting SVG through the `*Page` /
  `*Node` primitives; `SegoeUiMetrics` supplies the text metrics, `Metrics` the recovered XMLSpy
  layout constants and `Labels` the label truncation. Depends on `model` and `parsing`.
- Export is SVG or PNG. SVG is the reference page emitted by the
  `*Page` / `*Node` primitives; PNG is the same SVG rasterized by
  `SvgRasterizer` (Apache Batik) at its intrinsic `width`/`height` scaled by `--scale`. PNG
  rendering always forces `textLength` (independent of `--text-length`) so the metric-computed
  layout survives a missing Segoe UI fallback. A JPEG raster export is a possible future feature.
- Supported page shapes: simple element, chain, composite, context, nested, extension, lone
  type node and element-rooted nested context. `xs:all`, `xs:any` wildcards, inline anonymous
  types and group references all render; a top-level group root and a nested group mixed with
  attributes are the remaining shapes that `PageRenderer` returns an empty `Optional` for.
  Anchors live in `src/test/resources/golden/` (conformance pages in `conformance/`).
- An `xs:extension` base becomes a synthetic TYPE node (`TypeItem.isExtensionBase()`) that
  holds the inherited attributes and content; the derived type keeps only its own additions.
  `ExtensionPage` draws the base as a yellow container with a solid gray border,
  labelled `<prefix>:<Base>` in gray plus `(extension)` in gray regular, with the derived
  attributes (in their own `attributes` tab, `AttributeBox`) and content below it.
  Only type roots use this shape; an element whose type is an extension still renders through
  the context page. When the base/derived content expands, `NestedPage.renderExtension`
  reproduces the same shape with recursive base and derived groups.
- `AttributeBox` is the shared notched `attributes` tab (composite bodies, extension
  derived attributes and context pages). Context pages render the referenced type's attributes
  inside the yellow container, above its content group.
- Property labels carry the schema prefix of their namespace, rendered as a gray `prefix:`
  run followed by the black local name (attributes are unqualified). Root labels stay bare and
  type containers are entirely gray.
- The occurrence indicator is three runs (min, `..`, max); an unbounded maximum uses Segoe
  UI's `∞` glyph (`U+221E`). XMLSpy instead uses the `Symbol` font's `¥`, which only renders on
  Windows, so we deviate deliberately for portability. A prohibited (`maxOccurs="0"`) element
  is crossed out.
- The attribute container heading is the lowercase italic `attributes`.

## CLI quirks

- Argument parsing uses picocli in `cli/CliOptions.java` with standard syntax: short
  options (`-o file`, `-o=file`), long options (`--output file`, `--output=file`), and
  POSIX clustering (`-e3`). `-o/--output` and `-s/--stdout` form a required,
  mutually-exclusive group; the input `FILE` is required. There is no legacy normalization.
- The usage/help text is generated by picocli from `@Command`/`@Option` annotations
  (`-h/--help`, `-V/--version`). `Main.main` calls `System.exit(Main.run(args))`, so the
  process reports the real exit code (parse/runtime failure = 1); tests call `Main.run`
  directly to stay in-process.
- Console mode only; there is no GUI. `-s/--stdout` streams the diagram to stdout and
  suppresses informational logging via `ConsoleIO`. Preserve this; `CliRenderTest`
  captures raw stdout bytes.
- `-r/--root` is **required** and must resolve to exactly one component, otherwise the CLI
  reports an error (no overview/multi-root mode). A reference is a bare `NAME`, a
  `prefix:NAME` (prefix resolved from the loaded schemas or a `-N prefix=URI` binding) or a
  Clark-style `{namespace-uri}NAME`; `-k/--kind` forces `element`/`complexType`/`simpleType`/`group`.
  Without `-k` the match spans every kind, so an `element` and a `type` with the same name are
  ambiguous and must be disambiguated. `RootSelectorTest` covers the syntax.
- `-e/--expand` (default 1) is the expansion depth: how many levels of the `DiagramNode`
  tree are revealed before rendering. Level 2 and beyond expand each revealed element into a
  type container (named type) or nested column (inline type); `-e 0` shows only the root.
- `-d/--documentation` shows documentation (hidden by default). `-l/--language` selects an
  `xml:lang` entry (with a language-less/first fallback); `--all-languages` concatenates
  every entry. Internally `null` selects the default entry and `""` concatenates all;
  passing `-l ""` is rejected in favour of `--all-languages`, and `-l` with
  `--all-languages` together is rejected.
- `--text-length` pins every text run to its computed width via `textLength`; useful when the
  viewer lacks Segoe UI. Note Inkscape ignores embedded `@font-face` fonts, so do not reach for
  font embedding to fix viewer rendering.
- `--scale FACTOR` (default `1.0`) multiplies the PNG raster's pixel dimensions; it is rejected
  for SVG output and must be positive. PNG output is otherwise intrinsic size.
- `-o/--output` selects the format from its extension; `-f/--format svg|png` overrides it (a
  conflicting known extension is rejected, an unusual one is kept as-is). Without `-f` an
  unknown extension is rejected and an absent one gets the format's extension. `-s` defaults to
  svg and needs `-f png` for a PNG. The extension is read from the file name only, so dots in a
  directory do not matter, and a missing parent directory is created. PNG is streamed as raw
  bytes to stdout. `CliRenderTest`/`CliCornerCaseTest` assert the PNG signature and the IHDR
  dimensions (matching the SVG), plus run-to-run determinism, rather than byte-exact image
  goldens.
- Credentials come from `-u/--username` / `-p/--password`, `--password-file` (one trailing
  newline is stripped) or the `JXSD_USERNAME`/`JXSD_PASSWORD` environment variables, with the
  command line winning; `-p` together with `--password-file` is rejected and a password without
  a username is ignored with a warning.
- `--insecure` disables the external-content policy for trusted legacy sources: it allows
  plain-HTTP dependencies (also with credentials) and DTD/external-entity processing. It is
  off by default and prints a stderr warning when used; secure processing (expansion limits)
  stays enabled.
