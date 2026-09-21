# jxsd

[![Java](https://img.shields.io/badge/Java-21-green?logo=openjdk&logoColor=white)](https://www.java.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/vitalyzotov/jxsd/ci.yml?branch=main)](https://github.com/vitalyzotov/jxsd/actions)

Java 21 command-line tool that renders XSD components as **SVG** (or **PNG**) diagrams.
The rendered pages follow the visual conventions of Altova XMLSpy.

## Features

- Renders elements, complex/simple types, model groups, group references and wildcards
- Page shapes: simple element, chain, composite, context, nested, extension and lone type
- Recursive expansion (`-e`) of element children into type containers or nested columns
- Documentation (`xml:lang` aware) and occurrence indicators
- Deterministic, font-independent layout
- SVG output plus PNG export rasterized with Apache Batik

## Requirements

- Java 21 or newer
- Maven 3.8 or newer

## Build

```sh
mvn package
```

Produces the shaded CLI jar `target/jxsd-*.jar`. The full quality gate is
`mvn verify`, which runs the test suite plus Checkstyle, SpotBugs and the JaCoCo
coverage check (≥ 80 % line coverage).

## Usage

```sh
java -jar target/jxsd-*.jar -o out.svg -r Book -e 2 input.xsd
```

The `-h` flag prints the full generated help; the essentials:

| Option | Meaning |
| --- | --- |
| `-o, --output FILE` | output file; the extension selects `svg`/`png` |
| `-s, --stdout` | stream the diagram to stdout (SVG by default, `-f png` for PNG) |
| `-f, --format svg` / `png` | force the output format |
| `-r, --root REF` | root component: `NAME`, `prefix:NAME` or `{namespace-uri}NAME` (required) |
| `-N, --namespace PREFIX=URI` | bind a namespace prefix (repeatable) |
| `-k, --kind KIND` | force the root kind: `element`, `complexType`, `simpleType`, `group` |
| `-e, --expand N` | expansion depth (default 1; `-e 0` shows only the root) |
| `-d, --documentation` | show documentation |
| `-l, --language LANG` | preferred documentation language (`xml:lang`) |
| `--all-languages` | concatenate every documentation language |
| `--text-length` | pin text runs to their computed widths |
| `--scale FACTOR` | PNG scale factor (default 1.0) |
| `-u, --username` / `-p, --password` / `--password-file` | credentials for secured dependencies |
| `--insecure` | allow plain-HTTP dependencies and DTD processing (trusted legacy sources only) |

Ready-made example schemas live under `src/test/resources/reference/`:

```sh
java -jar target/jxsd-*.jar -o book.svg -r Book -e 2 \
  src/test/resources/reference/library.core.v1.xsd
```

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Altova and XMLSpy are trademarks of Altova GmbH. This project is not affiliated with,
endorsed by, or sponsored by Altova GmbH.

For contributors, the architecture and golden-output rules are documented in [AGENTS.md](AGENTS.md).
