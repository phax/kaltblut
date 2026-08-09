# ZUGFeRD / Factur-X — PDF carrier requirements comparison

Cross-version comparison of the PDF-only technical requirements
captured in the per-version files. XML-side rules (cardinalities,
EN 16931 business terms, code lists) are out of scope.

How to extend this file when a new ZUGFeRD version is published:

1. Add the new version as a column in every comparison table below.
2. Add a row to "1. Version mapping" linking ZUGFeRD ↔ Factur-X ↔
   release date ↔ per-version MD file.
3. If the new version introduces a brand-new mechanism (e.g. a new
   carrier format, a new extension schema URI, a new code list), add a
   dedicated subsection at the end (§ Δ from version X) rather than
   editing the older columns.
4. If the new version *removes* something, keep the row and mark
   "removed" in the new column — historical context matters for
   readers maintaining legacy invoices.

Empty cells (`—`) mean "not specified in that version's documents", not
"prohibited". Cells marked `n/a` mean the concept did not exist yet.

---

## 1. Version mapping

| ZUGFeRD | Factur-X      | Release date | Doc file                            | Notes                                                          |
| ------- | ------------- | ------------ | ----------------------------------- | -------------------------------------------------------------- |
| 1.0     | n/a           | 2014         | [1.0.md](1.0.md)                    | Pure ZUGFeRD; profiles BASIC/COMFORT/EXTENDED; URI uses `ferd` namespace and prefix `zf` |
| 2.0.1   | n/a           | 2019         | [2.0.1.md](2.0.1.md)                | DE-only ZUGFeRD-2.0.1 spec; namespace switches to `zugferd` with version `2p0`; prefix becomes `fx`; XML file renamed to `zugferd-invoice.xml`; adds MINIMUM and BASIC WL, renames COMFORT → EN 16931 (COMFORT) |
| 2.1     | 1.0.05        | 2020-03-24   | [2.1.md](2.1.md)                    | First aligned ZUGFeRD ↔ Factur-X release; primary namespace becomes `factur-x.eu` (`fx:`); legacy `zugferd.de` (`zf:`) retained |
| 2.2     | 1.0.06        | 2022-03-01   | [2.2.md](2.2.md)                    | XRECHNUNG reference profile added; PDF/A-4f option introduced; profile×country AFRelationship matrix consolidated |
| 2.3     | 1.0.07        | 2024-09-18   | [2.3.md](2.3.md)                    | XML moves to UN/CEFACT CII D22B; attachment list adds XLSX and ODS |
| 2.3.2   | 1.07.2        | 2024-11-15   | [2.3.2.md](2.3.2.md)                | `BR-HYBRID-*` business-rule block added                       |
| 2.3.3   | 1.07.3        | 2025-05-15   | [2.3.3.md](2.3.3.md)                | Code-list refresh; BR-HYBRID table rendered as image in PDF (text unchanged) |
| 2.4     | 1.08          | 2025-12-04   | [2.4.md](2.4.md)                    | Code-list refresh; carrier rules unchanged vs 2.3.2 / 2.3.3   |
| 2.5     | 1.09          | 2026-06-10   | [2.5.md](2.5.md)                    | XML-side update (new EXTENDED BTs anticipating Revised EN 16931 2026); carrier-side Δ: attachment whitelist gains `XLS` |
| 2.5.2   | 1.09.2        | 2026-08-04   | [2.5.2.md](2.5.2.md)                | XML-side Schematron maintenance; carrier-side Δ: new BR-FX-DE-04 (Warning, DE) in the HYBRID appendix. (1.09.1, 2026-06-11, was an EXTENDED-Schematron delivery hotfix without a republished spec document.) |

---

## 2. Carrier format

| Item                                  | 1.0                  | 2.0.1                | 2.1                | 2.2                 | 2.3                 | 2.3.2               | 2.3.3               | 2.4                 | 2.5                 | 2.5.2               |
| ------------------------------------- | -------------------- | -------------------- | ------------------ | ------------------- | ------------------- | ------------------- | ------------------- | ------------------- | ------------------- | ------------------- |
| PDF/A-3 mandatory                     | yes                  | yes                  | yes                | yes                 | yes                 | yes                 | yes                 | yes                 | yes                 | yes                 |
| PDF/A-4f also accepted                | no                   | no                   | no                 | yes (option)        | yes (option)        | yes (option)        | yes (option)        | yes (option)        | yes (option)        | yes (option)        |
| Conformance levels 3a/3b/3u accepted  | yes (any)            | yes (any)            | yes (any)          | yes; 3a recommended | yes; 3a recommended | yes; 3a recommended | yes; 3a recommended | yes; 3a recommended | yes; 3a recommended | yes; 3a recommended |
| PDF filename convention               | none                 | none                 | none               | none                | none                | none                | none                | none                | none                | none                |
| PDF/A-3 image whitelist stated in prose | yes (CCITT/JBIG2/JPEG/JP2K) | not explicit | not explicit | yes (CCITT/JBIG2/JPEG/JP2K) | yes | yes | yes | yes | yes | yes |

## 3. Embedded structured invoice file — naming

| Item                                  | 1.0                       | 2.0.1                  | 2.1                                                      | 2.2 → 2.5.2                                                |
| ------------------------------------- | ------------------------- | ---------------------- | -------------------------------------------------------- | ---------------------------------------------------------- |
| Default file name                     | `ZUGFeRD-invoice.xml`     | `zugferd-invoice.xml`  | `factur-x.xml` (primary) or `zugferd-invoice.xml` (legacy) | `factur-x.xml`                                             |
| XRECHNUNG reference profile name      | n/a                       | n/a                    | n/a (profile not yet introduced)                         | `xrechnung.xml` (when XRECHNUNG profile is used)           |
| MIME type                             | `text/xml`                | `text/xml`             | `text/xml`                                               | `text/xml`                                                 |

## 4. File specification dictionary / embedding

| Item                                                  | 1.0  | 2.0.1 | 2.1   | 2.2  | 2.3  | 2.3.2 | 2.3.3 | 2.4  | 2.5  | 2.5.2 |
| ----------------------------------------------------- | ---- | ----- | ----- | ---- | ---- | ----- | ----- | ---- | ---- | ----- |
| `/Params` recommended                                 | yes  | yes   | yes (conditional) | yes  | yes  | yes   | yes   | yes  | yes  | yes   |
| `/ModDate` required inside `/Params`                  | yes  | yes   | yes   | yes  | yes  | yes   | yes   | yes  | yes  | yes   |
| Empty `/Params` allowed                               | —    | no    | no    | no   | no   | no    | no    | no   | no   | no    |
| Listed in `/Names → /EmbeddedFiles`                   | yes  | yes   | yes   | yes  | yes  | yes   | yes   | yes  | yes  | yes   |
| `/Kids` tree levels under `/EmbeddedFiles` permitted  | yes  | yes   | yes   | yes  | yes  | yes   | yes   | yes  | yes  | yes   |
| `/AF` array on Document Catalogue (Root)              | yes  | yes   | yes   | yes  | yes  | yes   | yes   | yes  | yes  | yes   |
| Embedded XML relation level                           | document | document | document | document | document | document | document | document | document | document |

## 5. AFRelationship matrix

`A` = `Alternative`, `S` = `Source`, `D` = `Data`. Country splits begin
in 2.1 (FR introduced via Factur-X). Earlier ZUGFeRD-only versions
specify rules from the German tax perspective only.

| Profile                | 1.0 | 2.0.1 | 2.1 FR | 2.1 DE | 2.2 FR     | 2.2 DE | 2.3 FR     | 2.3 DE | 2.3.2 FR   | 2.3.2 DE | 2.3.3 FR   | 2.3.3 DE | 2.4 FR     | 2.4 DE | 2.5 FR     | 2.5 DE | 2.5.2 FR   | 2.5.2 DE |
| ---------------------- | --- | ----- | ------ | ------ | ---------- | ------ | ---------- | ------ | ---------- | -------- | ---------- | -------- | ---------- | ------ | ---------- | ------ | ---------- | -------- |
| MINIMUM                | n/a | D     | D      | D      | D          | D      | D          | D      | D          | D        | D          | D        | D          | D      | D          | D      | D          | D        |
| BASIC WL               | n/a | D     | D      | D      | D          | D      | D          | D      | D          | D        | D          | D        | D          | D      | D          | D      | D          | D        |
| BASIC                  | A   | A     | S or D | A      | A or S or D| A      | A or S or D| A      | A or S or D| A        | A or S or D| A        | A or S or D| A      | A or S or D| A      | A or S or D| A        |
| COMFORT / EN 16931     | A   | A     | S or D | A      | A or S or D| A      | A or S or D| A      | A or S or D| A        | A or S or D| A        | A or S or D| A      | A or S or D| A      | A or S or D| A        |
| EXTENDED               | A   | A     | S or D | A      | A or S or D| A      | A or S or D| A      | A or S or D| A        | A or S or D| A        | A or S or D| A      | A or S or D| A      | A or S or D| A        |
| XRECHNUNG              | n/a | n/a   | n/a    | n/a    | n/a (FR)   | A      | n/a (FR)   | A      | n/a (FR)   | A        | n/a (FR)   | A        | n/a (FR)   | A      | n/a (FR)   | A      | n/a (FR)   | A        |

Notes:

- In 1.0 there is no profile-dependent AFRelationship — every profile
  embeds with `Alternative`.
- In 2.0.1 the profile-dependent split appears (Data for MINIMUM/BASIC
  WL, Alternative for BASIC/COMFORT/EXTENDED); `Source` is permitted
  when the PDF was derived from the XML.
- The COMFORT profile (1.0, 2.0.1) is renamed EN 16931 (COMFORT) in 2.1
  and EN 16931 from 2.2 onward.

## 6. PDF/A XMP extension schema (primary)

| Item                                                | 1.0                                                       | 2.0.1                                                  | 2.1 (primary)                                          | 2.2 → 2.5.2                                            |
| --------------------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------ | ------------------------------------------------------ |
| Schema name (table value)                           | `ZUGFeRD PDFA Extension Schema` (also `ZUGFeRD Schema` in XML comment) | `ZUGFeRD PDFA Extension Schema`                  | `ZUGFeRD PDFA Extension Schema` (worked example: `Factur-x PDFA Extension Schema`) | `Factur-X PDFA Extension Schema` (capitalisation varies)|
| Namespace URI                                       | `urn:ferd:pdfa:CrossIndustryDocument:invoice:1p0#`        | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#`  | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#` | `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#` |
| Schema prefix                                       | `zf`                                                      | `fx`                                                   | `fx`                                                   | `fx`                                                   |
| `DocumentType` value                                | `INVOICE`                                                 | `INVOICE`                                              | `INVOICE`                                              | `INVOICE`                                              |
| `DocumentFileName`                                  | `ZUGFeRD-invoice.xml`                                     | `zugferd-invoice.xml`                                  | `factur-x.xml`                                         | `factur-x.xml` (or `xrechnung.xml` for XRECHNUNG)      |
| `Version` value                                     | `1.0`                                                     | `2p0`                                                  | `1p0` (table) / `1.0` (worked example)                 | `1.0`                                                  |
| `ConformanceLevel` permitted values                 | BASIC, COMFORT, EXTENDED                                  | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED           | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED           | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG |
| Extension schema embedded in XMP packet             | required                                                  | required                                               | required                                               | required                                               |
| URI must end with `#`                               | yes                                                       | yes                                                    | yes                                                    | yes                                                    |

## 7. PDF/A XMP extension schema — legacy entries

A "legacy" entry is one retained for backward compatibility with an
older spec version. Files may carry one of the schema variants below.

| Item                                                | 2.1 legacy (Supplement B)                              | 2.2 → 2.5.2 (legacy)                                   |
| --------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------ |
| Schema name                                         | `ZUGFeRD PDFA Extension Schema`                        | `ZUGFeRD PDF/A Extension Schema`                       |
| Namespace URI                                       | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#`  | `urn:zugferd:pdfa:CrossIndustryDocument:invoice:1p0#`  |
| Schema prefix                                       | `zf`                                                   | `zf`                                                   |
| `DocumentType` value                                | `INVOICE`                                              | `INVOICE`                                              |
| `DocumentFileName`                                  | `zugferd-invoice.xml`                                  | `zugferd-invoice.xml`                                  |
| `Version` value                                     | `2p0`                                                  | `2p0`                                                  |
| `ConformanceLevel` permitted values                 | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED           | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED           |
| Status                                              | active alternative for 2.1                             | "marked as deprecated, … may change in future"         |

Note: the legacy URI's version segment changes from `2p0#` (in 2.1
Supplement B) to `1p0#` (in 2.2 onward). The 2.1 supplement still
treats `zugferd.de` as fully active; 2.2 onward marks it deprecated.

## 8. Inconsistency to be aware of (BR-HYBRID-04)

From 2.3.2 onward, the textual rule `BR-HYBRID-04` states the URI
SHALL be `urn:factur-x:pdfa:CrossIndustryDocument:1p0#` (without the
`:invoice` segment). §6.3.1 of the same documents and the worked
example both use `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#`.
Treat §6.3.1 as authoritative for actual files until an erratum is
published.

## 9. Additional attachments (other than the invoice XML)

| Item                                       | 1.0                                                              | 2.0.1                                                                                             | 2.1                                                                                                                         | 2.2                                                  | 2.3                                                | 2.3.2                                              | 2.3.3                                              | 2.4                                                | 2.5                                                     | 2.5.2                                                   |
| ------------------------------------------ | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------- | -------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------- |
| Allowed formats                            | PDF, TXT, GIF, TIFF, JPG, CSV, XML (receiver must handle these)  | Profile-dependent: BASIC/EN/BASIC WL/MIN: PDF, PNG, JPEG, CSV, XLSX, ODS. EXTENDED: any MIME      | Profile-dependent: BASIC/EN/MIN/BASIC WL: PDF, PNG, JPEG, CSV/Text, XLSX, ODS. EXTENDED: any valid MIME                     | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON             | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, XLSX, ODS | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, XLSX, ODS | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, XLSX, ODS | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, XLSX, ODS | PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, **XLS**, XLSX, ODS | same as 2.5                                             |
| Extra XMP metadata for non-invoice attachments | not required                                                  | not required                                                                                      | not required                                                                                                                | not required                                         | not required                                       | not required                                       | not required                                       | not required                                       | not required                                            | not required                                            |
| Attachment referencing convention          | —                                                                | relative URL `#ef=<filename>` in `AdditionalReferencedDocument/URIID` (PDF fragment ID, RFC 8118) | relative URL `#ef=<filename>` (same)                                                                                        | reserved names: EDIFACT `factur-xedifact.edi`, UBL `factur-xubl.xml` | same as 2.2                                        | same as 2.2                                        | same as 2.2                                        | same as 2.2                                        | same as 2.2                                             | same as 2.2                                             |

## 10. BR-HYBRID-* business rule block

Severity levels: `F` = Fatal, `W` = Warning, `I` = Info.

| Rule            | Severity | Applies to | First appears in        | Notes                                                                                  |
| --------------- | -------- | ---------- | ----------------------- | -------------------------------------------------------------------------------------- |
| BR-HYBRID-01    | I        | FR + DE    | 2.3.2 (Factur-X 1.07.2) | Hybrid = machine-readable XML + human-readable PDF envelope.                           |
| BR-HYBRID-02    | F        | FR + DE    | 2.3.2                   | PDF envelope SHALL be PDF/A-3 (PDF/A-4f optionally allowed).                           |
| BR-HYBRID-03    | F        | FR + DE    | 2.3.2                   | PDF/A XMP extension schema with the prescribed structure SHALL be used.                |
| BR-HYBRID-04    | F        | FR + DE    | 2.3.2                   | Extension schema URI (see §8 note).                                                    |
| BR-HYBRID-05    | F        | FR + DE    | 2.3.2                   | Schema namespace prefix SHALL be `fx`.                                                 |
| BR-HYBRID-06    | F        | FR + DE    | 2.3.2                   | `fx:DocumentType` value SHALL come from *HybridDocumentType*.                          |
| BR-HYBRID-07    | F        | FR + DE    | 2.3.2                   | `fx:ConformanceLevel` value SHALL come from *HybridConformanceType*.                   |
| BR-HYBRID-08    | F        | FR + DE    | 2.3.2                   | `fx:DocumentFileName` value SHALL come from *HybridDocumentFilename*.                  |
| BR-HYBRID-09    | F        | FR + DE    | 2.3.2                   | `fx:Version` value SHALL come from *HybridDocumentVersion*.                            |
| BR-HYBRID-10    | W        | FR + DE    | 2.3.2                   | `fx:Version` SHOULD be `1.0`.                                                          |
| BR-HYBRID-11    | W        | FR + DE    | 2.3.2                   | `/AFRelationship` SHOULD follow the profile × country matrix.                          |
| BR-HYBRID-12    | F        | FR + DE    | 2.3.2                   | XML embedding method SHALL conform to the specification (extractability).              |
| BR-HYBRID-13    | F        | FR + DE    | 2.3.2                   | Embedded file name SHALL be from *HybridDocumentFilename*.                             |
| BR-HYBRID-14    | W        | FR + DE    | 2.3.2                   | Embedded file name SHOULD match `fx:DocumentFileName`.                                 |
| BR-HYBRID-15    | W        | FR + DE    | 2.3.2                   | `fx:ConformanceLevel` SHOULD match the embedded XML profile.                           |
| BR-HYBRID-DE-01 | F        | DE         | 2.3.2                   | DE↔DE: MINIMUM profile SHALL NOT be used.                                              |
| BR-HYBRID-DE-02 | F        | DE         | 2.3.2                   | DE↔DE: BASIC WL profile SHALL NOT be used.                                             |
| BR-HYBRID-FR-01 | F        | FR         | 2.3.2                   | FR↔FR: XRECHNUNG profile SHALL NOT be used.                                            |
| BR-FX-DE-01     | F        | DE         | 2.3.2                   | DE: supporting docs for place/time/kind SHALL be in BG-24 (no technical check).        |
| BR-FX-DE-02     | F        | DE         | 2.3.2                   | DE: main services SHALL be detailed in the XML (no technical check).                   |
| BR-FX-DE-03     | W        | DE↔DE      | 2.3.2                   | DE↔DE: from 2025-01-01 the XML is the invoice; PDF/A technical errors → Warning.       |
| BR-FX-DE-04     | W        | DE         | 2.5.2 (Factur-X 1.09.2) | DE: non-down-payment invoices (type code ≠ 386) need BT-72, BG-14 or BG-26 per item; without BT-72 at least BT-80. XML-content rule, no carrier check possible. |

In 1.0, 2.0.1, 2.1, 2.2 and 2.3 the carrier rules are expressed only
in prose; they are equivalent in substance but not given identifiers.
The textual table in 2.3.2 has remained unchanged in 2.3.3, 2.4 and
2.5 — confirmed by direct extraction of the selectable text in 2.3.2,
2.4 and 2.5 (in 2.3.3 the table is image-rendered and not extractable).
2.5.2 leaves all existing rows untouched and appends BR-FX-DE-04
(confirmed the same way against the 1.09 and 1.09.2 texts).

## 11. Visual representation & securing (prose, recommendations)

| Item                                                                                | 1.0 | 2.0.1 | 2.1  | 2.2 → 2.5.2 |
| ----------------------------------------------------------------------------------- | --- | ----- | ---- | ----------- |
| Single-page / multi-page good-practice layouts                                      | —   | —     | —    | yes (§5.2) |
| Profile logos for visual identification                                             | —   | —     | —    | yes (§6.5) |
| Securing modes: qualified e-sign/seal **or** documented reliable audit trail        | —   | —     | yes  | yes (§4)   |
| EDI mode for the XML part only when `AFRelationship` ∈ {`Alternative`, `Source`}    | —   | —     | —    | yes (§4)   |

## 12. Versioning conventions

| Item                                              | 1.0                                              | 2.0.1                                                                 | 2.1                                                                       | 2.2 → 2.5.2                                                      |
| ------------------------------------------------- | ------------------------------------------------ | --------------------------------------------------------------------- | ------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| Spec version                                      | 1.0                                              | 2.0.1                                                                 | 1.0.05 (Factur-X) / 2.1 (ZUGFeRD)                                         | 1.0.06 → 1.0.07 → 1.07.2 → 1.07.3 → 1.08 → 1.09 → 1.09.2         |
| `fx:Version` (or `zf:Version`) value              | `1.0` (`zf:`)                                    | `2p0` (`fx:` with ZUGFeRD URI)                                        | `1p0` / `1.0`                                                             | `1.0` (BR-HYBRID-10 Warning if anything else)                    |
| URN segment used                                  | `1p0`                                            | `2p0`                                                                 | `1p0` (primary `factur-x.eu`) / `2p0` (legacy `zugferd.de`)               | `1p0` (primary)                                                  |
| XML BT-24 spec ID (BASIC, e.g.)                   | n/a (pre-EN 16931)                               | `urn:cen.eu:en16931:2017#compliant#urn:zugferd.de:2p0:basic`          | `urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic`             | `urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic`    |
| Validation artefacts versioning                   | —                                                | per profile                                                           | per profile                                                               | per profile; third-level for bug-fix (e.g. 1.08.3, 1.09.x)       |
| Upward compatibility                              | n/a                                              | within major                                                          | within major                                                              | within `1.zz` family                                             |

## 13. Profiles per version

| Version | Profiles supported                                                                                |
| ------- | ------------------------------------------------------------------------------------------------- |
| 1.0     | BASIC, COMFORT, EXTENDED                                                                          |
| 2.0.1   | MINIMUM, BASIC WL, BASIC, EN 16931 (COMFORT), EXTENDED                                            |
| 2.1     | MINIMUM, BASIC WL, BASIC, EN 16931 (COMFORT), EXTENDED                                            |
| 2.2     | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, **XRECHNUNG** (reference profile)                   |
| 2.3     | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |
| 2.3.2   | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |
| 2.3.3   | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |
| 2.4     | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |
| 2.5     | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |
| 2.5.2   | MINIMUM, BASIC WL, BASIC, EN 16931, EXTENDED, XRECHNUNG                                           |

## 14. At-a-glance evolution of identifier strings

If you only need to recognise a hybrid invoice from its XMP, here are
the namespace URIs you will encounter in the wild, in chronological
order:

1. `urn:ferd:pdfa:CrossIndustryDocument:invoice:1p0#` — ZUGFeRD 1.0
2. `urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#` — ZUGFeRD 2.0
   / 2.0.1 / 2.1-legacy (Supplement B)
3. `urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#` — ZUGFeRD 2.1
   primary, ZUGFeRD 2.2 → 2.5.2
4. `urn:zugferd:pdfa:CrossIndustryDocument:invoice:1p0#` — legacy
   variant tolerated in ZUGFeRD 2.2 → 2.5.2 (note the version segment
   regressed from `2p0` to `1p0`)
5. `urn:factur-x:pdfa:CrossIndustryDocument:1p0#` — appears only in
   BR-HYBRID-04 rule text from 2.3.2 onward, apparent transcription
   error vs §6.3.1

Embedded XML file names you will encounter:

- `ZUGFeRD-invoice.xml` — ZUGFeRD 1.0
- `zugferd-invoice.xml` — ZUGFeRD 2.0 / 2.0.1 / 2.1-legacy
- `factur-x.xml` — ZUGFeRD 2.1 primary onward
- `xrechnung.xml` — only with the XRECHNUNG reference profile in
  ZUGFeRD 2.2 onward

## 15. Δ from 2.4 to 2.5 (Factur-X 1.09)

Carrier-side delta is minimal. Captured separately to keep the older
columns stable and so anyone maintaining 1.zz tooling can see the
narrow footprint of the 1.09 update.

- **Attachment whitelist gains `XLS`** (legacy Excel binary). The §6.4
  bullet list reads `PDF, TXT, GIF, TIFF, JPG, CSV, XML, JSON, XLS,
  XLSX, ODS` in 1.09. 1.08 listed only `XLSX`.
- **`BR-HYBRID-*` rule block is byte-identical** to 2.3.2 / 2.3.3 / 2.4
  (text, severity, country applicability — verified by extracting the
  selectable text from both 2.4 and 2.5 PDFs).
- **AFRelationship matrix unchanged.**
- **XMP namespace URI unchanged.** A 2.5 PDF is indistinguishable from a
  2.4 PDF on namespace alone — the spec generation is only visible from
  the BT-24 specification identifier inside the embedded XML (XML side,
  out of carrier scope).
- **`fx:Version` value unchanged** (`1.0`).
- **§6.6 editorial inconsistency** — the heading still reads "Factur-X
  **1.08** maintenance and validation artefacts" and the body still
  says "*which is 1.08 for this current version*". Copy-paste oversight
  from 2.4; the document title, footer running header, and version
  history table on page 8 are all unambiguously 1.09. Treat 1.08 here
  as a typo for 1.09 pending an erratum.
- **All other Δ are XML-side** — new EXTENDED business terms
  (BT-173 → BT-180, BT-193, BT-215, BT-216, BG-34, BG-X-94, BT-X-591/-592),
  EXTENDED cardinality relaxations, BR-FXEXT-* / BR-FXEXT-CO-* churn
  anticipating the Revised EN 16931 2026, ID CTC FR code updates,
  EN 16931 code-list refresh (v17b, valid from 2026-05-15). None of
  these change anything for a PDF-carrier validator like kaltblut.

## 16. Δ from 2.5 to 2.5.2 (Factur-X 1.09.2)

Maintenance release dated 2026-08-04. (The intermediate 1.09.1 of
2026-06-11 was a hotfix for an EXTENDED-Schematron delivery issue and
did not republish the specification document.) Carrier-side, verified
by extracting and diffing the selectable text of the 1.09 and 1.09.2
PDFs:

- **Spec sections 1–6 are textually identical** to 1.09 — carrier
  format, embedding, XMP extension schema, AFRelationship matrix,
  attachment whitelist, securing: no change. The §6.6 "1.08" heading
  typo and the BR-HYBRID-04 URI inconsistency both persist.
- **One new rule in the HYBRID appendix (3.d): BR-FX-DE-04**
  (Warning, DE) — non-down-payment invoices (type code ≠ 386) must
  carry BT-72 "Actual delivery date", BG-14 "Invoicing period" or
  BG-26 "Invoice line period" per item; without BT-72 at least the
  country of delivery (BT-80). This is an XML-content rule; like
  BR-FX-DE-01/-02 it cannot be checked at the carrier level. All other
  rows of the BR-HYBRID table are unchanged.
- **All other Δ are XML-side Schematron maintenance** — BR-CO-27
  renamed CII-SR-470; EXTENDED BT-151 / BT-151-0 cardinality relaxed to
  0..1 (with BR-FXEXT-CO-04 as backstop); BR-S-1 / BR-Z-1 / BR-E-1 /
  BR-AE-1 / BR-IC-1 / BR-G-1 / BR-O-1 / BR-AF-1 / BR-AG-1 replaced by
  BR-FXEXT-*-01 equivalents in EXTENDED; BR-54 split into
  BR-FXEXT-BR-54-1/-2; BR-FXEXT-06 / BR-FXEXT-08 corrections;
  CII-SR-463 to CII-SR-476 added per profile; all Schematron rules now
  carry a BR ID or `FX-SCH-A-XXXXXX` identifier. Irrelevant to the PDF
  carrier.
