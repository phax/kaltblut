# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

Kaltblut is a Java toolkit for **ZUGFeRD / Factur-X** hybrid invoices that does three things:
detect the flavor/profile of a hybrid PDF, extract the embedded XML and supporting attachments,
and validate the carrier-side (PDF-side) specification rules — including PDF/A-3 conformance via
veraPDF.

What it is **not**: XML business-rule validation (EN 16931 cardinalities, code lists, KoSIT
XRechnung rules) and PDF creation/embedding are explicitly out of scope. For XML rules, use
[phive-rules-zugferd](https://github.com/phax/phive-rules).

The per-version requirements analysis under `docs/requirements/` (and especially
`docs/requirements/comparison.md`) is the source of truth for which carrier rules apply per
ZUGFeRD/Factur-X generation — consult it before changing detection, extraction, or BR-HYBRID-*
validation logic.

## Build & test

Requires Java 17+ and Maven. Standard Maven from the repo root operates on all modules:

```shell
mvn clean install           # full build + tests, installs to local repo
mvn clean package           # build + tests, no install (also produces the CLI fat JAR)
mvn -pl kaltblut-core test  # tests for a single module
mvn -pl kaltblut-core -Dtest=HybridValidatorTest test            # single test class
mvn -pl kaltblut-core -Dtest=HybridValidatorTest#testFoo test    # single test method
```

CI (`.github/workflows/maven.yml`) builds on JDK 17, 21, and 25; 17 is the deploy target.

The CLI fat JAR comes out of `mvn clean package` at
`kaltblut-cli/target/kaltblut-cli-full.jar`. Main class is
`com.helger.kaltblut.cli.KaltblutMain`; subcommands are `inspect`, `extract`, `attachments`,
`validate`.

## Module layout & dependency direction

Four modules, strictly layered (parent POM at repo root):

```
kaltblut-testfiles   ← classpath-resource locator + sample PDFs (test-scope dep of others)
        ↑
kaltblut-core        ← library: source / model / inspect / extract / validate / pdfbox
        ↑                                                              ↑
kaltblut-verapdf     ← veraPDF-backed IPdfA3ValidatorSPI               │
        ↑                                                              │
kaltblut-cli         ← picocli command-line client, depends on both ───┘
```

- `kaltblut-core` has zero dependency on veraPDF; it discovers a PDF/A-3 validator via
  `ServiceLoader<IPdfA3ValidatorSPI>`. If no implementation is on the classpath, validation
  records a single `INFORMATION` finding and proceeds.
- `kaltblut-verapdf` registers `VeraPdfA3ValidatorSPI` via
  `META-INF/services/com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI`. It uses the veraPDF
  `-jakarta` artifact line (JAXB 4.x / `jakarta.xml.bind`) — do **not** introduce the legacy
  `javax.xml.bind` 2.x line.
- `kaltblut-testfiles` exposes `KaltblutTestFiles.*` constants pointing at one representative PDF
  per spec generation under `external/zugferd/`. New tests should reuse these rather than
  ship new sample PDFs.

## Core architecture (kaltblut-core)

Package map under `com.helger.kaltblut.core`:

| Package    | Role                                                                                       |
| ---------- | ------------------------------------------------------------------------------------------ |
| `source`   | `IHybridSource` + `HybridSource` factories. Byte-array-centric input abstraction.          |
| `model`    | Immutable value objects: `EZugferdFlavor`, `EZugferdProfile`, `EAFRelationship`, `EZugferdCountry`, `HybridMetadata`, `HybridAttachment`. |
| `pdfbox`   | `HybridDocument` — internal PDFBox 3 wrapper that opens the PDF once and exposes XMP + `/AF` data. The only package that touches `org.apache.pdfbox.*`. |
| `inspect`  | Tier 1 — `HybridInspector` (flavor detection + XMP metadata).                              |
| `extract`  | Tier 2 — `HybridExtractor` (invoice XML, named attachment, full attachment list).          |
| `validate` | Tier 3 — `HybridValidator`, `HybridValidatorSettings`, `HybridValidationResult`, `HybridFinding`, `EHybridSeverity`, `IPdfA3ValidatorSPI`. |

Conceptual flow: caller builds an `IHybridSource` (one of seven factories on `HybridSource` —
file / path / bytes / ByteBuffer / URL / InputStream / classpath), then passes it to an
inspector/extractor/validator. All public entry points take `IHybridSource`; PDFBox is an
implementation detail behind `HybridDocument` and never appears in the public API.

Key design choices to preserve:

- **`IHybridSource` is byte-array-centric**: the contract is `byte[] getBytes()` plus
  diagnostic `getSize()` / `getName()`. PDFBox 3 needs random access and every operation
  eventually needs the full PDF in memory, so single-read inputs aren't worth a parallel
  streaming API. Implementations may read lazily and cache; callers must not mutate the returned
  array.
- **Model classes are immutable value objects.** Mutability lives only in
  `HybridValidatorSettings` (configuration) and the `HybridValidator` itself (which is
  `@NotThreadSafe`).
- **PDF/A-3 validation is pluggable** via `IPdfA3ValidatorSPI` + `ServiceLoader`. Only the
  first registered implementation is used.
- **BR-HYBRID-* rule set follows Factur-X 1.07.2 / ZUGFeRD 2.3.2** — the first version that
  numbered the rules. Earlier generations expressed the same requirements in prose; the
  validator applies them uniformly. Country-specific variants exist (BR-HYBRID-DE-*,
  BR-HYBRID-FR-*) driven by `EZugferdCountry`.

## Coding conventions specific to this codebase

The user's global rules (`~/.claude/rules/`) define naming and formatting conventions
(Hungarian notation, `m_`/`s_` field prefixes, space before parens, ID always uppercase, etc.).
Project-specific points on top of those:

- **ph-commons is the standard library.** Use `ICommonsList` / `CommonsArrayList`, `ValueEnforcer`,
  `StreamHelper`, etc. Do not introduce Guava or Apache Commons Collections as alternatives.
- **JSpecify annotations** (`@NonNull`, `@Nullable`) — not `javax.annotation.*`, not Lombok.
- **`@ReturnsMutableCopy`** when returning collections so callers know they own the result.
- **Logger pattern**: `private static final Logger LOGGER = LoggerFactory.getLogger (X.class);`
  with inline string concatenation (no SLF4J `{}` placeholders). See the global naming rule.
- **Apache 2.0 license header** on every Java file (copyright Philip Helger, 2026).
- **No `serialVersionUID`** anywhere.
- **No `@Override` on interface methods** — only on overrides of concrete/abstract superclass
  methods or default-method overrides.

## Working on requirements / cross-version logic

When changing detection, extraction, or BR-HYBRID-* validation behaviour, cross-check against
`docs/requirements/comparison.md` (the cross-version master table) and the relevant per-version
file (`docs/requirements/<version>.md`). The README's "Supported Versions" table is a quick
summary of XMP namespace ↔ embedded-XML-filename mappings — keep it in sync if you change either.

## Releases & versioning

Current version is `0.9.0-SNAPSHOT` (parent POM). Parent: `com.helger:parent-pom:3.0.3`.
Snapshots are deployed by CI from JDK 17 builds on push via the `release-snapshot` profile.
