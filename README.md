# flugesel

A Java toolkit for working with **ZUGFeRD / Factur-X** hybrid invoices: detect the flavor of any
hybrid PDF, extract the embedded XML and supporting attachments, and validate the carrier-side
specification rules — including PDF/A-3 conformance via [veraPDF](https://verapdf.org/).

XML-side business rules (cardinalities, EN 16931 rules, code lists, …) are out of scope for this
project; use [phive-rules-zugferd](https://github.com/phax/phive-rules) for those.

Per-version requirements analysis used to design this library lives under
[`docs/requirements/`](docs/requirements/). See
[`docs/requirements/comparison.md`](docs/requirements/comparison.md) for a cross-version overview
of every PDF carrier rule from ZUGFeRD 1.0 (2014) through Factur-X 1.08 / ZUGFeRD 2.4 (2025-12-04).

## Supported Versions

The detection table covers every published release since 2014:

| ZUGFeRD | Factur-X | XMP namespace URI                                              | Embedded XML name           |
| ------- | -------- | -------------------------------------------------------------- | --------------------------- |
| 1.0     | n/a      | `urn:ferd:pdfa:CrossIndustryDocument:invoice:1p0#`             | `ZUGFeRD-invoice.xml`       |
| 2.0.1   | n/a      | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#`          | `zugferd-invoice.xml`       |
| 2.1     | 1.0.05   | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml`              |
| 2.1     | 1.0.05   | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#` (legacy) | `zugferd-invoice.xml`       |
| 2.2     | 1.0.06   | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml` / `xrechnung.xml` |
| 2.2     | 1.0.06   | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:1p0#` (legacy) | `zugferd-invoice.xml`       |
| 2.3     | 1.0.07   | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml` / `xrechnung.xml` |
| 2.3.2   | 1.07.2   | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml` / `xrechnung.xml` |
| 2.3.3   | 1.07.3   | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml` / `xrechnung.xml` |
| 2.4     | 1.08     | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`         | `factur-x.xml` / `xrechnung.xml` |

## What flugesel Does (and Does Not Do)

| Tier | Concern                                                                                     | Status      |
| ---- | ------------------------------------------------------------------------------------------- | ----------- |
| 1    | **Detection & metadata**: flavor, profile, XMP fields, embedded-file name, `/AFRelationship` | implemented |
| 2    | **Extraction**: invoice XML, named attachments, full attachment list                        | implemented |
| 3    | **Validation**: BR-HYBRID-* business rules, PDF/A-3 (via veraPDF SPI)                       | implemented |
| 4    | Creation / embedding XML to produce hybrid PDFs                                             | not in scope |
| —    | XML business-rules validation (EN 16931, KoSIT XRechnung)                                   | use [phive-rules-zugferd](https://github.com/phax/phive-rules) |

## Project Layout

This is a multi-module Maven project:

- `flugesel-core` — the library. Source abstraction, model, inspector, extractor, validator.
- `flugesel-verapdf` — PDF/A-3 validation adapter that wires veraPDF to the
  `IPdfA3Validator` SPI. Optional; pull it in only if you need PDF/A-3 conformance checks.
- `flugesel-cli` — the command-line client (picocli). Builds a standalone fat JAR.

## Key Library Concepts

### `IHybridSource` — the input abstraction

All public entry points take a source. Use one of the `HybridSource` factories:

```java
import com.helger.flugesel.source.HybridSource;
import com.helger.flugesel.source.IHybridSource;

IHybridSource s1 = HybridSource.fromFile (new File ("invoice.pdf"));
IHybridSource s2 = HybridSource.fromPath (Path.of ("invoice.pdf"));
IHybridSource s3 = HybridSource.fromBytes (aPdfBytes);
IHybridSource s4 = HybridSource.fromByteBuffer (aBuffer);
IHybridSource s5 = HybridSource.fromUrl (new URL ("https://example.com/invoice.pdf"));

// Single-read: the InputStream is consumed once and then the source is exhausted.
IHybridSource s6 = HybridSource.fromInputStreamOnce (aIS);

// Materialise a single-read stream up front so it can be passed to multiple flugesel ops.
IHybridSource s7 = HybridSource.materialize (aIS);
```

`IHybridSource` extends `com.helger.base.io.iface.IHasInputStream`, so any existing
`IHasInputStream` can be used wherever an `IHybridSource` is expected by adapting it through a
factory, and `source.isReadMultiple()` tells you whether it is re-readable. Operations that need
to read the PDF more than once internally use `HybridSource.ensureReadMultiple(...)` to upgrade a
single-read source to an in-memory byte array when necessary.

### Model

The model classes in `com.helger.flugesel.model` are immutable value objects:

- `EZugferdFlavor` — namespace-URI fingerprint of the spec generation.
- `EProfile` — `MINIMUM`, `BASIC_WL`, `BASIC`, `COMFORT`, `EN_16931`, `EXTENDED`, `XRECHNUNG`.
- `EAFRelationship` — `Data`, `Source`, `Alternative`, `Supplement`, `Unspecified`.
- `ECountry` — `DE`, `FR`, `OTHER` (drives country-specific rules).
- `HybridMetadata` — single snapshot of XMP fields + `/AF` data.
- `HybridAttachment` — name, MIME type, AFRelationship, ModDate, bytes, invoice-XML flag.

## Usage

### Tier 1: detection

```java
import com.helger.flugesel.inspect.HybridInspector;
import com.helger.flugesel.model.EZugferdFlavor;
import com.helger.flugesel.model.HybridMetadata;

IHybridSource aSource = HybridSource.fromFile (new File ("invoice.pdf"));

if (HybridInspector.isHybridInvoice (aSource))
{
  EZugferdFlavor eFlavor = HybridInspector.detectFlavor (aSource);
  HybridMetadata aMeta = HybridInspector.readMetadata (aSource);
  System.out.println ("Flavor:        " + aMeta.getFlavor ());
  System.out.println ("Profile:       " + aMeta.getProfile ());
  System.out.println ("Embedded file: " + aMeta.getEmbeddedFileName ());
  System.out.println ("AFRelationship: " + aMeta.getAFRelationship ());
}
```

### Tier 2: extraction

```java
import com.helger.flugesel.extract.HybridExtractor;
import com.helger.flugesel.model.HybridAttachment;

byte [] aXmlBytes = HybridExtractor.extractInvoiceXml (aSource);
List <HybridAttachment> aAttachments = HybridExtractor.listAttachments (aSource);
byte [] aExcel = HybridExtractor.extractAttachment (aSource, "list_of_measurement.xlsx");
```

### Tier 3: validation

```java
import com.helger.flugesel.model.ECountry;
import com.helger.flugesel.validate.Finding;
import com.helger.flugesel.validate.HybridValidator;
import com.helger.flugesel.validate.ValidationResult;

HybridValidator aValidator = new HybridValidator ();
aValidator.getSettings ()
          .setCountry (ECountry.DE)
          .setCheckPdfA3 (true)
          .setApplyDePdfADowngrade (true);

ValidationResult aResult = aValidator.validate (aSource);
if (!aResult.isValid ())
  for (Finding aF : aResult.getFindings (com.helger.flugesel.validate.ESeverity.FATAL))
    System.out.println (aF);
```

PDF/A-3 validation runs via the `IPdfA3Validator` SPI. Add `flugesel-verapdf` to the classpath
to enable veraPDF; without it `validate()` records a single `INFORMATION` finding noting that
PDF/A-3 conformance was not checked.

### Command line

Build the standalone fat JAR and run it:

```bash
mvn clean package
java -jar flugesel-cli/target/flugesel-cli-full.jar [subcommand] [options] <files...>
```

Subcommands:

| Subcommand    | Description                                                                          |
| ------------- | ------------------------------------------------------------------------------------ |
| `inspect`     | Print flavor, profile, XMP fields, embedded-file name, and `/AFRelationship`.        |
| `extract`     | Write the embedded invoice XML to disk.                                              |
| `attachments` | List all embedded files (invoice XML + supporting documents).                        |
| `validate`    | Run BR-HYBRID-* business rules and PDF/A-3 validation. Exit code 0 iff no fatal findings. |

Common options:

| Option                                | Subcommand | Description                                                            | Default     |
| ------------------------------------- | ---------- | ---------------------------------------------------------------------- | ----------- |
| `-o`, `--output-dir`                  | `extract`  | Directory to write XML files to                                        | `.`         |
| `-s`, `--suffix`                      | `extract`  | Output filename suffix                                                 | `-invoice`  |
| `-c`, `--country`                     | `validate` | `DE`, `FR`, or `OTHER` — drives country-specific rules                 | `OTHER`     |
| `--no-pdfa`                           | `validate` | Skip PDF/A-3 validation via the SPI                                    | off         |
| `--no-de-pdfa-downgrade`              | `validate` | Disable the BR-FX-DE-03 downgrade for DE↔DE invoices                   | off         |
| `-h`, `--help`                        | all        | Show help                                                              |             |
| `-V`, `--version`                     | all        | Show version                                                           |             |

Examples:

```bash
# Detect the flavor of one or more PDFs
java -jar flugesel-cli-full.jar inspect invoice.pdf another-invoice.pdf

# Extract the invoice XML to /tmp/out/
java -jar flugesel-cli-full.jar extract -o /tmp/out invoice.pdf

# List all embedded files in a PDF
java -jar flugesel-cli-full.jar attachments invoice.pdf

# Validate a DE↔DE invoice (PDF/A-3 errors downgraded per BR-FX-DE-03)
java -jar flugesel-cli-full.jar validate -c DE invoice.pdf

# Validate without PDF/A-3 (fast path; only the BR-HYBRID-* rules run)
java -jar flugesel-cli-full.jar validate --no-pdfa invoice.pdf
```

## Building

Requires Java 17+ and Maven.

```bash
mvn clean package
```

The build produces (replacing `x.y.z` with the effective version):

- `flugesel-core/target/flugesel-core-x.y.z-SNAPSHOT.jar` — core library JAR.
- `flugesel-verapdf/target/flugesel-verapdf-x.y.z-SNAPSHOT.jar` — veraPDF adapter JAR.
- `flugesel-cli/target/flugesel-cli-x.y.z-SNAPSHOT.jar` — CLI library JAR.
- `flugesel-cli/target/flugesel-cli-full.jar` — standalone executable fat JAR (all dependencies bundled).

## Maven Coordinates

```xml
<!-- Core library: detection + extraction + BR-HYBRID validation -->
<dependency>
  <groupId>com.helger.flugesel</groupId>
  <artifactId>flugesel-core</artifactId>
  <version>x.y.z</version>
</dependency>

<!-- Optional: veraPDF-backed PDF/A-3 validation -->
<dependency>
  <groupId>com.helger.flugesel</groupId>
  <artifactId>flugesel-verapdf</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Extending

To plug in a different PDF/A-3 validator (or none at all), implement
`com.helger.flugesel.validate.IPdfA3Validator` and register the class via
`META-INF/services/com.helger.flugesel.validate.IPdfA3Validator`. The validator is discovered via
`ServiceLoader`; only the first implementation found is used.

## License

Apache License, Version 2.0.

## News and Noteworthy

v0.1.0 - 2026-05-11 (in development)
* Initial scaffold based on the cross-version requirements analysis in `docs/requirements/`.
* Tier 1 detection: recognises all five XMP extension-schema namespaces seen across ZUGFeRD
  1.0, 2.0.1, 2.1, 2.2, 2.3, 2.3.2, 2.3.3 and 2.4.
* Tier 2 extraction: invoice XML, named attachments, full attachment list including ModDate and
  MIME type.
* Tier 3 validation: BR-HYBRID-01 through BR-HYBRID-15 (and the BR-HYBRID-DE-*/-FR-* country
  variants) plus PDF/A-3 conformance via the `IPdfA3Validator` SPI implemented by
  `flugesel-verapdf` using veraPDF 1.26.x.
* `IHybridSource` abstraction with re-readability tracking; sources can be created from
  `byte[]`, `ByteBuffer`, `File`, `Path`, `URL`, `InputStream` (one-shot or materialised).
* Command-line client with subcommands `inspect`, `extract`, `attachments`, `validate`.
