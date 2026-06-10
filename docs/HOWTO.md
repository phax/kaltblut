# How to extend `docs/requirements/` when a new ZUGFeRD / Factur-X release lands

Captures the actual workflow used for the 2.5 / Factur-X 1.09 update. Use
this as a checklist next time a new spec ships so the per-version files
stay narrow and the comparison table stays honest.

## Scope reminder

kaltblut is a **PDF-carrier** library. Every file under
`docs/requirements/` is scoped to the PDF side:

- carrier format (PDF/A-3, PDF/A-4f)
- XMP extension schema (namespace URI, prefix, fields)
- embedded-file mechanics (file name, MIME, file-spec dict, `/AF`,
  AFRelationship)
- additional-attachment whitelist
- BR-HYBRID-* business-rule block
- visual-representation prose and securing modes

XML business terms, cardinalities, code lists, BR-CO-*, BR-FXEXT-*,
EN 16931 schematron — **out of scope**. Note them in a short "Out of
scope" section at the end of the per-version file and link to
phive-rules-zugferd, then move on. Do **not** clutter the comparison
table with XML-side detail.

## What the official package contains

Each Factur-X release is shipped by FNFE-MPE / FeRD as a single ZIP
with this canonical layout (filenames track the current version
numbers):

```
ZF<NN>_EN.zip
├── Documentation/
│   ├── 0_FACTUR-X_<x.yy>_<YYYY_MM_DD>_EN.pdf      ← the spec
│   ├── 1_FACTUR-X <x.yy> - <date> - EN FR.xlsx    ← full data dictionary
│   ├── 2_EN16931 code lists values v<NN> - ... .xlsx
│   ├── 3_..._Profile_MINIMUM.pdf                   ← technical appendices
│   ├── 4_..._Profile_BASIC_WL.pdf                  ← (one per profile)
│   ├── 5_..._Profile_BASIC.pdf
│   ├── 6_..._Profile_EN16931.pdf
│   └── 7_..._Profile_EXTENDED.pdf
└── Examples/
    ├── 0. MINIMUM/
    ├── 1. BASIC WL/
    ├── 2. BASIC/
    ├── 3. EN16931/
    └── 4. EXTENDED/
```

There is a corresponding `ZF<NN>_DE.zip` with German prose; the EN
package is enough for our purposes (the BR-HYBRID rules are in English
in both).

## Step 1 — Extract just what you need

Don't try to read the whole spec PDF; it is 80–90 pages and 90 % of it
is XML semantics. Extract the searchable text once and grep:

```shell
pdftotext -layout 0_FACTUR-X_<x.yy>_<date>_EN.pdf spec.txt
```

If extraction errors out with "disk full" complaints, use `/tmp` or a
spot with real space — `pdftotext` writes large layouted output.

## Step 2 — Read these sections, in this order

The PDF-carrier rules are concentrated in **§3 Principles**, **§4
Securing**, **§5.2 Presentation**, and **§6 Implementation in PDF/A-3**.
Skip everything else on the first pass.

| Section | What to look for                                                   | Goes into                                        |
| ------- | ------------------------------------------------------------------ | ------------------------------------------------ |
| §3 Principle 1 (+ footnote) | PDF/A-3 mandate, optional PDF/A-4f      | §1 "Carrier format"                              |
| §3 Principle 2, 4 | Visual-vs-XML consistency expectations            | §5 "Visual representation"                       |
| §4         | Securing modes (signature/seal, audit trail, EDI for XML only) | §6 "Securing the PDF envelope"                  |
| §5.2       | Good-practice presentation                                        | §5 "Visual representation"                       |
| §6 intro   | PDF/A-3 carrier characteristics                                   | §1 "Carrier format"                              |
| §6.1       | PDF/A-3 derived constraints (fonts, images, encryption, …)        | §1 sub-list                                      |
| §6.2       | XML embedding, MIME, /Params, ModDate, Names tree, /AF            | §2.2 / §2.3                                      |
| §6.2.1     | "document level" associated-file rule                              | §2.3                                             |
| §6.2.2     | AFRelationship values + the **profile × country matrix**          | §2.4 (matrix)                                    |
| §6.3.1     | Factur-X 1.0 XMP extension schema (URI, prefix, fields)            | §3                                               |
| §6.3.2     | Legacy ZUGFeRD 2.0 XMP extension schema                            | §3 "Legacy" subsection                           |
| §6.4       | Additional-attachment whitelist + reserved names                  | §4                                               |
| §6.5       | Profile logos                                                     | §5 (one line)                                    |
| §6.6       | Versioning, validation artefacts                                  | §8 "Versioning"                                  |
| Appendix "3.d Business Rules for HYBRID Documents" | BR-HYBRID-* table       | §7 (transcribe in full as a markdown table)      |

Read in that order — by the end of §6 you have ~90 % of the per-version
file; the BR-HYBRID appendix gives the rest.

## Step 3 — Diff against the previous version

The cheapest reliable diff is the selectable-text dump from the
previous and current PDF, narrowed to §6 and the BR-HYBRID appendix:

```shell
# pull just §6 and the BR-HYBRID block from each
sed -n '/^6      Implementation/,/^7      Presentation/p' spec-prev.txt > prev-s6.txt
sed -n '/^6      Implementation/,/^7      Presentation/p' spec-cur.txt  > cur-s6.txt
diff -u prev-s6.txt cur-s6.txt | less
```

(Adjust the section anchors — they sometimes move by one heading.) For
the BR-HYBRID block, the appendix table is usually selectable text in
the modern PDFs (2.3.2, 2.4, 2.5) but rendered as an image in 2.3.3 —
note that explicitly when applicable.

What to focus on in the diff:

1. **Section 6 prose changes** — these often reword without changing
   substance; flag only the substantive ones.
2. **The AFRelationship matrix** — copy the new table; if values differ,
   that's a big deal.
3. **The attachment whitelist** (§6.4 bullet list) — XLS was added in
   2.5; this is the kind of small change that's easy to miss.
4. **The BR-HYBRID-* table** — compare row-by-row, including severity
   and country applicability. Verify the actual text, not just the
   count.
5. **§6.6** — versioning prose; FNFE-MPE has been known to leave the
   previous version number in the heading (2.5 still says "Factur-X
   1.08 maintenance…"). Call these out.

## Step 4 — Write the per-version file

Copy the previous version's `.md` as a starting point and edit. The
canonical section order is fixed; keep it.

1. Carrier format
2. Embedded structured invoice file (filename, MIME, file-spec dict,
   AFRelationship matrix)
3. PDF/A XMP extension schema (primary + legacy)
4. Additional attachments
5. Visual representation
6. Securing
7. BR-HYBRID-* quick reference
8. Versioning
9. Profiles (one-line recap)
10. (Optional) "What this means for kaltblut"
11. (Optional) "Out-of-scope changes" — short XML-side bullet list for
    cross-referencing.

A few conventions used across the existing files:

- Cite **§n.n** numbers in parentheses next to every claim. Future-you
  will want to re-verify.
- Quote rule IDs verbatim (`BR-HYBRID-04 (Fatal)` — never just "the
  fatal one").
- Where the spec contradicts itself (BR-HYBRID-04 vs §6.3.1 URI), say
  so explicitly and pick a winner. Don't paper over.
- When a release is byte-identical to the previous one in some
  respect, say so explicitly — that's information.

## Step 5 — Update `comparison.md`

Add the new version as **a new column** in every table that has
year-by-year columns (Carrier format, File specification, AFRelationship
matrix, BR-HYBRID, Attachments, Profiles).

For the consolidated "2.2 → 2.4" / "2.2 → 2.5" columns: just bump the
range end if behaviour is unchanged. Only split out a fresh column when
behaviour actually diverges.

If the new release introduces a brand-new mechanism (new carrier
format, new XMP URI, new code list), add a dedicated **§ Δ from
version X** subsection at the end (see §15 for 2.5) rather than
rewriting older columns — historical context matters.

Empty cells (`—`) mean "not specified in that version's documents",
not "prohibited". Cells marked `n/a` mean the concept did not exist
yet. Preserve this distinction.

## Step 6 — Verify against the example PDFs

Open the sample PDFs in `Examples/` from the new ZIP. For each:

1. Confirm the embedded XML filename (`factur-x.xml` / `xrechnung.xml`
   / `zugferd-invoice.xml`) matches what §6.2 says.
2. Confirm the XMP `fx:Version` matches §6.3.1.
3. Confirm AFRelationship is one of the matrix-allowed values for the
   profile.

If the sample PDFs are byte-identical to the previous release's samples
(which 2.5 shipped — same MD5 as 2.4), note it. That's hard evidence
nothing substantive changed at the carrier level.

```shell
md5sum old/BASIC-WL_Einfach_fx.pdf new/BASIC-WL_Einfach_fx.pdf
```

## Step 7 — Touch the code only where required

The flavor enum (`EZugferdFlavor`) is keyed on the XMP namespace URI.
If the new release reuses an existing URI (true for every release from
2.1 onward), there is **no new enum entry**. Update the class-level
Javadoc to extend the "covered through …" range, and that's it.

The validator's rule set follows 2.3.2's BR-HYBRID-* numbering — only
touch it if a BR ID was added or removed (none in 2.5).

Other touch points:

- `README.md` "Supported Versions" table — append a row.
- `README.md` lead paragraph naming the latest version date.
- `KaltblutTestFiles` — if the new sample is byte-different from the
  previous one, add a new constant; if byte-identical (2.5 case),
  mention this in the Javadoc on the existing constant.

## Step 8 — Run the build

```shell
mvn -pl kaltblut-core,kaltblut-testfiles -am clean compile -DskipTests
```

This catches typos in any Javadoc references and confirms nothing
inadvertently broke.

## Anti-patterns

- **Don't paraphrase BR-HYBRID rules into your own words.** Quote them
  verbatim. The "must / shall / should" verbs carry severity.
- **Don't promote prose into a table without §-citations.** If you
  can't cite the section, you don't know it yet.
- **Don't auto-merge identical columns in the comparison table.** A
  visible "2.4 yes / 2.5 yes" tells readers it was rechecked, not
  assumed.
- **Don't update README without updating the per-version `.md`.**
  README is a summary; the source of truth is `docs/requirements/`.
- **Don't add code for a release that reuses every existing URI and
  rule ID.** kaltblut's flavor enum is XMP-namespace-keyed; if nothing
  new appears at that level, only docs change.
