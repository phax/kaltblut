# Kaltblut

A Java toolkit for working with **ZUGFeRD / Factur-X** hybrid invoices: detect the flavor of any
hybrid PDF, extract the embedded XML and supporting attachments, and validate the carrier-side
specification rules — including PDF/A-3 conformance via [veraPDF](https://verapdf.org/).

XML-side business rules (cardinalities, EN 16931 rules, code lists ...) are out of scope for this
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

## Why "Kaltblut"?

**Kaltblut** is German for the family of heavy draft horse breeds — the strongest and steadiest of the *Zugpferde*.
The name is a nod to ZUGFeRD, which German speakers hear as Zugpferd ("draft horse").
Kaltblut picks the most workmanlike of them: built to pull heavy loads calmly and reliably, much like this toolkit aims to handle ZUGFeRD invoices.

As a bonus, *kaltblütig* in everyday German also means "cool-headed" — a useful trait for any library dealing with tax-relevant invoice processing

## What Kaltblut Does (and Does Not Do)

| Tier | Concern                                                                                     | Status      |
| ---- | ------------------------------------------------------------------------------------------- | ----------- |
| 1    | **Detection & metadata**: flavor, profile, XMP fields, embedded-file name, `/AFRelationship` | implemented |
| 2    | **Extraction**: invoice XML, named attachments, full attachment list                        | implemented |
| 3    | **Validation**: BR-HYBRID-* business rules, PDF/A-3 (via veraPDF SPI)                       | implemented |
| 4    | Creation / embedding XML to produce hybrid PDFs                                             | not in scope |
| —    | XML business-rules validation (EN 16931, KoSIT XRechnung)                                   | use [phive-rules-zugferd](https://github.com/phax/phive-rules) |

## Project Layout

This is a multi-module Maven project:

- `kaltblut-core` — the library. Source abstraction, model, inspector, extractor, validator.
  Classes live under `com.helger.kaltblut.core.*`.
- `kaltblut-testfiles` — shared test fixtures (sample PDFs + classpath-resource locator).
- `kaltblut-verapdf` — PDF/A-3 validation adapter that wires veraPDF to the
  `IPdfA3ValidatorSPI` SPI. Optional; pull it in only if you need PDF/A-3 conformance checks.
- `kaltblut-cli` — the command-line client (picocli). Builds a standalone fat JAR.

## Key Library Concepts

### `IHybridSource` — the input abstraction

All public entry points take a source. Use one of the `HybridSource` factories:

```java
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.core.source.IHybridSource;

IHybridSource s1 = HybridSource.fromFile (new File ("invoice.pdf"));         // lazy + cached
IHybridSource s2 = HybridSource.fromPath (Path.of ("invoice.pdf"));          // lazy + cached
IHybridSource s3 = HybridSource.fromBytes (aPdfBytes);                       // wraps array
IHybridSource s4 = HybridSource.fromByteBuffer (aBuffer);                    // copies
IHybridSource s5 = HybridSource.fromUrl (new URL ("https://example.com/invoice.pdf"));
IHybridSource s6 = HybridSource.fromInputStream (aIS);                       // reads now, closes
IHybridSource s7 = HybridSource.fromClasspath ("samples/invoice.pdf");       // resource path
```

`IHybridSource` is byte-array-centric: the contract is `byte[] getBytes() throws IOException`,
plus `long getSize()` and `String getName()` as diagnostic hints. PDFBox 3 needs random access
and every Kaltblut operation eventually needs the complete PDF in memory, so distinguishing
single-read from multi-read inputs added API surface without value. Implementations may read
lazily on first call and cache the result; callers must not mutate the returned array.

### Model

The model classes in `com.helger.kaltblut.core.model` are immutable value objects:

- `EZugferdFlavor` — namespace-URI fingerprint of the spec generation.
- `EZugferdProfile` — `MINIMUM`, `BASIC_WL`, `BASIC`, `COMFORT`, `EN_16931`, `EXTENDED`, `XRECHNUNG`.
- `EAFRelationship` — `Data`, `Source`, `Alternative`, `Supplement`, `Unspecified`.
- `EZugferdCountry` — `DE`, `FR`, `OTHER` (drives country-specific BR-HYBRID rules).
- `HybridMetadata` — single snapshot of XMP fields + `/AF` data.
- `HybridAttachment` — name, MIME type, AFRelationship, ModDate, bytes, invoice-XML flag.

## Usage

### Tier 1: detection

```java
import com.helger.kaltblut.core.inspect.HybridInspector;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.model.HybridMetadata;

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
import com.helger.kaltblut.core.extract.HybridExtractor;
import com.helger.kaltblut.core.model.HybridAttachment;

byte [] aXmlBytes = HybridExtractor.extractInvoiceXml (aSource);
List <HybridAttachment> aAttachments = HybridExtractor.listAttachments (aSource);
byte [] aExcel = HybridExtractor.extractAttachment (aSource, "list_of_measurement.xlsx");
```

### Tier 3: validation

```java
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.validate.HybridFinding;
import com.helger.kaltblut.core.validate.HybridValidator;
import com.helger.kaltblut.core.validate.ValidationResult;

HybridValidator aValidator = new HybridValidator ();
aValidator.getSettings ()
          .setCountry (EZugferdCountry.DE)
          .setCheckPdfA3 (true)
          .setApplyDePdfADowngrade (true);

ValidationResult aResult = aValidator.validate (aSource);
if (!aResult.isValid ())
  for (HybridFinding aF : aResult.getFindings (com.helger.kaltblut.core.validate.EHybridSeverity.FATAL))
    System.out.println (aF);
```

PDF/A-3 validation runs via the `IPdfA3ValidatorSPI` SPI. Add `kaltblut-verapdf` to the classpath
to enable veraPDF; without it `validate()` records a single `INFORMATION` finding noting that
PDF/A-3 conformance was not checked.

### Command line

Build the standalone fat JAR and run it:

```shell
mvn clean package
java -jar kaltblut-cli/target/kaltblut-cli-full.jar [subcommand] [options] <files...>
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

```shell
# Detect the flavor of one or more PDFs
java -jar kaltblut-cli-full.jar inspect invoice.pdf another-invoice.pdf

# Extract the invoice XML to /tmp/out/
java -jar kaltblut-cli-full.jar extract -o /tmp/out invoice.pdf

# List all embedded files in a PDF
java -jar kaltblut-cli-full.jar attachments invoice.pdf

# Validate a DE↔DE invoice (PDF/A-3 errors downgraded per BR-FX-DE-03)
java -jar kaltblut-cli-full.jar validate -c DE invoice.pdf

# Validate without PDF/A-3 (fast path; only the BR-HYBRID-* rules run)
java -jar kaltblut-cli-full.jar validate --no-pdfa invoice.pdf
```

## Building

Requires Java 17+ and Maven.

```shell
mvn clean package
```

The build produces (replacing `x.y.z` with the effective version):

- `kaltblut-core/target/kaltblut-core-x.y.z-SNAPSHOT.jar` — core library JAR.
- `kaltblut-verapdf/target/kaltblut-verapdf-x.y.z-SNAPSHOT.jar` — veraPDF adapter JAR.
- `kaltblut-cli/target/kaltblut-cli-x.y.z-SNAPSHOT.jar` — CLI library JAR.
- `kaltblut-cli/target/kaltblut-cli-full.jar` — standalone executable fat JAR (all dependencies bundled).

## Maven Coordinates

```xml
<!-- Core library: detection + extraction + BR-HYBRID validation -->
<dependency>
  <groupId>com.helger.kaltblut</groupId>
  <artifactId>kaltblut-core</artifactId>
  <version>x.y.z</version>
</dependency>

<!-- Optional: veraPDF-backed PDF/A-3 validation -->
<dependency>
  <groupId>com.helger.kaltblut</groupId>
  <artifactId>kaltblut-verapdf</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Extending

To plug in a different PDF/A-3 validator (or none at all), implement
`com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI` and register the class via
`META-INF/services/com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI`. The validator is
discovered via `ServiceLoader`; only the first implementation found is used.

## License

Apache License, Version 2.0.

## News and Noteworthy

v0.9.0 - work in progress
* Detection: recognises all five XMP extension-schema namespaces seen across ZUGFeRD 1.0, 2.0.1, 2.1, 2.2, 2.3, 2.3.2, 2.3.3 and 2.4.
* Extraction: invoice XML, named attachments, full attachment list including Modification Date and MIME type.
* Validation: BR-HYBRID-01 through BR-HYBRID-15 (and the BR-HYBRID-DE-*/-FR-* country
  variants) plus PDF/A-3 conformance via the `IPdfA3ValidatorSPI` SPI implemented by
  `kaltblut-verapdf` using veraPDF (`-jakarta` artifact line, JAXB 4.x only).
* Command-line client with subcommands `inspect`, `extract`, `attachments`, `validate`.
