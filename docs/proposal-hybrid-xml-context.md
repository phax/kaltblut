# Proposal: `HybridXmlContext` — read routing-relevant identifiers from the embedded invoice XML

- **Status:** proposal / not yet implemented
- **Date:** 2026-08-02
- **Origin:** phorm discussion [#19](https://github.com/phax/phorm/discussions/19) ("National rule layers
  can't be reached when BT-24 only declares the format") + the EN 16931 BT-24 requirement.

> This document is self-contained. It captures the motivation, the exact design, the code sketch, the
> ph-commons APIs it relies on (already verified against local sources), and the open questions to
> resolve against real sample files. It is intended to be implemented in a separate session.

---

## 1. Problem / motivation

EN 16931 models **BT-24 "Specification identifier"** (UBL `cbc:CustomizationID`, CII
`ram:GuidelineSpecifiedDocumentContextParameter/ram:ID`) as *the* routing key: a single value naming
the total rule set the document conforms to. §7.6 of EN 16931-1 (ed8a) says the instance should carry
the assigned CIUS identifier so the receiver can "apply processing … in accordance with the rules under
which it was generated".

phorm discussion #19 shows where that breaks in practice: Factur-X / ZUGFeRD assign BT-24 values that
only encode the **format-profile tier** (`urn:factur-x.eu:1p0:{minimum,basic,extended}`), shared across
all jurisdictions. A French Factur-X invoice therefore routes to the German ZUGFeRD layer and the French
national rules (BR-FR-*) never fire. **Jurisdiction is not in the profile / BT-24 — it lives in the XML
business terms (Seller country, BT-40).** Reaching a national rule layer needs a *composite* key:
`profile tier + BT-24 + Seller country`.

Kaltblut sits entirely on the carrier side today and hits the same wall in three places its own code
already anticipates:

1. **`HybridValidator` BR-HYBRID-15** wants to check that `fx:ConformanceLevel` matches the embedded XML
   profile, but downgrades itself to `INFORMATION` with the comment *"Without parsing the XML's BT-24 we
   cannot cross-check"*.
2. **`EZugferdFlavor`** Javadoc notes ZUGFeRD 2.5 is indistinguishable at the XMP level and *"only the
   embedded XML's BT-24 specification identifier reveals it"* — which kaltblut does not read. (Context
   only: this proposal *surfaces* BT-24 but does not wire it into flavor detection — see §4.4.)
3. **`EZugferdCountry {DE, FR, OTHER}`** already gates the BR-HYBRID-DE-* / BR-HYBRID-FR-* rules and the
   DE PDF/A downgrade, but it is a **caller-supplied setting**. Its `OTHER` Javadoc even says
   "auto-detection failed", yet nothing auto-detects.

## 2. Proposal in one line

Add a small, **opt-in** reader that peeks into the embedded invoice XML for a handful of
identifier/context fields — BT-24, BT-23, and Seller country — surfaced as an immutable
`HybridXmlContext`. This is identifier extraction, **not** business-rule validation.

### The scope caveat (decision required)

Kaltblut's charter (CLAUDE.md / README) says XML business-rule validation is **out of scope**, and the
`HybridExtractor` Javadoc warns callers to bring their own XXE-safe parser. This proposal deliberately
steps up to — but not over — that line: it reads *identifier fields only* (no cardinalities, no code
lists, no EN rules), the same category as reading `fx:ConformanceLevel` from XMP, just from the XML side.
Because it is a genuine boundary decision, the capability is a **separate opt-in class** and is **off by
default** in the validator. Confirm this boundary is acceptable before implementing.

### The downstream payoff (phorm / discussion #19)

Once kaltblut surfaces `profile (XMP) + BT-24 + Seller country` when it cracks a hybrid PDF, a downstream
router such as phorm gets the exact composite key discussion #19 needs, without re-opening and
re-parsing the XML itself. Clean division of labour: kaltblut extracts routing context; phive/phorm does
the layered rule selection.

## 3. Country resolution — mirror PEPPOL, not just the postal address

A background analysis of `openpeppol/2026.5/PEPPOL-EN16931-UBL.sch` shows PEPPOL does **not** derive the
seller country from the postal address alone. Its `supplierCountry` `<let>` uses a priority chain and
treats the **VAT-registration country as authoritative over the postal address**:

1. First 2 chars of Seller `PartyTaxScheme[TaxScheme/ID='VAT']/CompanyID` (the VAT prefix)
2. else the **Tax representative's** VAT prefix
3. else `PostalAddress/Country/IdentificationCode`
4. else `'XX'`

This matters: a seller can be postally in one country but VAT-registered in another, and PEPPOL's
national rules key off the VAT country. If kaltblut resolved jurisdiction from postal country only, its
auto-detected country could disagree with the layer that actually applies — reintroducing the exact
mismatch we are trying to close. **`getResolvedCountry()` must therefore apply the VAT-prefix-first
chain**, and `HybridXmlContext` should expose both the VAT-derived and postal country codes.

Path mapping (UBL ↔ CII):

| Signal | UBL | CII |
| --- | --- | --- |
| BT-24 CustomizationID | `cbc:CustomizationID` | `…/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID` |
| BT-23 Business process | `cbc:ProfileID` | `…/ram:BusinessProcessSpecifiedDocumentContextParameter/ram:ID` |
| Seller VAT (country = first 2 chars) | `cac:AccountingSupplierParty/cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID` | `ram:SellerTradeParty/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='VA']` |
| Tax-rep VAT | `cac:TaxRepresentativeParty/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID` | `ram:SellerTaxRepresentativeTradeParty/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='VA']` |
| Seller postal country (BT-40) | `cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode` | `ram:SellerTradeParty/ram:PostalTradeAddress/ram:CountryID` |

## 4. Design

Three artifacts + optional validator wiring.

### 4.1 New value object — `model/HybridXmlContext.java`

```java
/* Apache 2.0 header, Copyright (C) 2026 Philip Helger ... */
package com.helger.kaltblut.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Routing-relevant context read from the embedded invoice XML payload (CII or UBL):
 * the BT-24 Specification identifier (CustomizationID), the BT-23 Business process type,
 * and the Seller country (VAT-derived and postal) used to resolve the applicable
 * {@link EZugferdCountry}.
 * <p>
 * This is identifier/context extraction, not business-rule validation: no cardinalities,
 * code lists or EN 16931 rules are evaluated. Missing values yield <code>null</code>.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridXmlContext
{
  private final String m_sSyntax;
  private final String m_sCustomizationID;
  private final String m_sBusinessProcessType;
  private final String m_sSellerVatCountryCode;
  private final String m_sSellerPostalCountryCode;
  private final EZugferdCountry m_eResolvedCountry;

  public HybridXmlContext (@Nullable final String sSyntax,
                           @Nullable final String sCustomizationID,
                           @Nullable final String sBusinessProcessType,
                           @Nullable final String sSellerVatCountryCode,
                           @Nullable final String sSellerPostalCountryCode,
                           @NonNull final EZugferdCountry eResolvedCountry)
  {
    m_sSyntax = sSyntax;
    m_sCustomizationID = sCustomizationID;
    m_sBusinessProcessType = sBusinessProcessType;
    m_sSellerVatCountryCode = sSellerVatCountryCode;
    m_sSellerPostalCountryCode = sSellerPostalCountryCode;
    m_eResolvedCountry = eResolvedCountry;
  }

  /** @return "CII" or "UBL"; the detected payload syntax, or <code>null</code> if unknown. */
  @Nullable
  public String getSyntax ()
  {
    return m_sSyntax;
  }

  /** @return BT-24 Specification identifier, or <code>null</code>. */
  @Nullable
  public String getCustomizationID ()
  {
    return m_sCustomizationID;
  }

  /** @return BT-23 Business process type, or <code>null</code>. */
  @Nullable
  public String getBusinessProcessType ()
  {
    return m_sBusinessProcessType;
  }

  /** @return the seller country derived from the VAT identifier prefix (first 2 chars), or
   *          <code>null</code>. */
  @Nullable
  public String getSellerVatCountryCode ()
  {
    return m_sSellerVatCountryCode;
  }

  /** @return the raw seller postal country code (BT-40), or <code>null</code>. */
  @Nullable
  public String getSellerPostalCountryCode ()
  {
    return m_sSellerPostalCountryCode;
  }

  /** @return the jurisdiction resolved via the PEPPOL VAT-prefix-first chain; never
   *          <code>null</code> ({@link EZugferdCountry#OTHER} when absent/unmapped). */
  @NonNull
  public EZugferdCountry getResolvedCountry ()
  {
    return m_eResolvedCountry;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Syntax", m_sSyntax)
                                       .append ("CustomizationID", m_sCustomizationID)
                                       .append ("BusinessProcessType", m_sBusinessProcessType)
                                       .append ("SellerVatCountryCode", m_sSellerVatCountryCode)
                                       .append ("SellerPostalCountryCode", m_sSellerPostalCountryCode)
                                       .append ("ResolvedCountry", m_eResolvedCountry)
                                       .getToString ();
  }
}
```

### 4.2 Addition to `model/EZugferdCountry.java`

```java
import java.util.Locale;
...
  /**
   * Resolve a raw ISO 3166-1 alpha-2 country code (e.g. from BT-40 Seller country or a VAT
   * identifier prefix) to the country gate. Unknown or <code>null</code> codes map to
   * {@link #OTHER}.
   *
   * @param sCountryCode
   *        the raw country code. May be <code>null</code>.
   * @return the resolved country; never <code>null</code>.
   */
  @NonNull
  public static EZugferdCountry getFromCountryCode (@Nullable final String sCountryCode)
  {
    final String sNormalized = sCountryCode == null ? null : sCountryCode.trim ().toUpperCase (Locale.ROOT);
    return getFromIDOrDefault (sNormalized, OTHER);
  }
```

### 4.3 New opt-in reader — `inspect/HybridXmlContextReader.java`

Bounded (reuses `HybridExtractor` limit enforcement), XXE-hardened, dispatches CII vs UBL on the root
local name, applies the VAT-first country chain.

```java
/* Apache 2.0 header ... */
package com.helger.kaltblut.core.inspect;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.kaltblut.core.extract.HybridExtractor;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.HybridXmlContext;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.Telemetry;
import com.helger.xml.EXMLParserFeature;
import com.helger.xml.XMLHelper;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.read.DOMReaderSettings;

/**
 * Optional reader that peeks into the embedded invoice XML for the few routing-relevant
 * context fields (BT-24, BT-23, Seller country). This is the one place in kaltblut that looks
 * at the XML payload; it reads identifiers only and performs no business-rule validation.
 * <p>
 * Targets modern CII (ZUGFeRD 2.x / Factur-X, root <code>rsm:CrossIndustryInvoice</code>) and
 * UBL (<code>Invoice</code> / <code>CreditNote</code>). ZUGFeRD 1.0's legacy
 * <code>CrossIndustryDocument</code> structure is not covered.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridXmlContextReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HybridXmlContextReader.class);

  private static final String CII_NS_RSM = "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100";
  private static final String CII_NS_RAM = "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100";
  private static final String UBL_NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
  private static final String UBL_NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";

  // Untrusted payload: harden against XXE / DTD / entity expansion.
  private static final DOMReaderSettings XML_SETTINGS = new DOMReaderSettings ().setFeatureValue (EXMLParserFeature.SECURE_PROCESSING,
                                                                                                  true)
                                                                                .setFeatureValue (EXMLParserFeature.DISALLOW_DOCTYPE_DECL,
                                                                                                  true)
                                                                                .setFeatureValue (EXMLParserFeature.EXTERNAL_GENERAL_ENTITIES,
                                                                                                  false)
                                                                                .setFeatureValue (EXMLParserFeature.EXTERNAL_PARAMETER_ENTITIES,
                                                                                                  false);

  private HybridXmlContextReader ()
  {}

  @NonNull
  public static HybridXmlContext readXmlContext (@NonNull final IHybridSource aSource) throws IOException
  {
    return readXmlContext (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * Read the routing context from the embedded invoice XML.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the context; never <code>null</code>. When no invoice XML is present or the syntax
   *         is unrecognised, all identifier getters return <code>null</code> and the resolved
   *         country is {@link EZugferdCountry#OTHER}.
   * @throws IOException
   *         on I/O / PDF / XMP failure or limit violation.
   */
  @NonNull
  public static HybridXmlContext readXmlContext (@NonNull final IHybridSource aSource,
                                                 @NonNull final HybridLimits aLimits) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    // Standalone entry point: opens the PDF once and extracts the invoice XML.
    final byte [] aXml = HybridExtractor.extractInvoiceXml (aSource, aLimits);
    return readXmlContext (aXml);
  }

  /**
   * Read the routing context from already-extracted invoice XML bytes. Callers that have already
   * opened the PDF (e.g. {@link com.helger.kaltblut.core.validate.HybridValidator}, which holds an
   * open document) should use this overload with the bytes they already extracted, to avoid opening
   * and parsing the PDF a second time.
   *
   * @param aXml
   *        the raw invoice XML bytes, or <code>null</code> if the PDF carries no invoice XML.
   * @return the context; never <code>null</code>. When <code>aXml</code> is <code>null</code> or the
   *         syntax is unrecognised, all identifier getters return <code>null</code> and the resolved
   *         country is {@link EZugferdCountry#OTHER}.
   */
  @NonNull
  public static HybridXmlContext readXmlContext (@Nullable final byte [] aXml)
  {
    // withSpanThrowing per CLAUDE.md telemetry convention (harmless when the lambda throws nothing
    // checked); this overload performs no I/O, so the method itself declares no 'throws'.
    return Telemetry.withSpanThrowing ("kaltblut.xmlcontext", ETelemetrySpanKind.INTERNAL, aSpan -> {
      final HybridXmlContext aCtx;
      if (aXml == null)
        aCtx = _empty ();
      else
      {
        final Document aDoc = DOMReader.readXMLDOM (aXml, XML_SETTINGS);
        if (aDoc == null)
        {
          LOGGER.warn ("Failed to parse embedded invoice XML for context extraction");
          aCtx = _empty ();
        }
        else
        {
          final Element eRoot = aDoc.getDocumentElement ();
          final String sLocal = eRoot == null ? null : eRoot.getLocalName ();
          if ("CrossIndustryInvoice".equals (sLocal))
            aCtx = _fromCII (eRoot);
          else
            if ("Invoice".equals (sLocal) || "CreditNote".equals (sLocal))
              aCtx = _fromUBL (eRoot);
            else
              aCtx = _empty ();
        }
      }

      aSpan.setAttribute ("kaltblut.xmlcontext.syntax", aCtx.getSyntax () != null ? aCtx.getSyntax () : "none");
      if (aCtx.getCustomizationID () != null)
        aSpan.setAttribute ("kaltblut.xmlcontext.customizationid", aCtx.getCustomizationID ());
      aSpan.setAttribute ("kaltblut.xmlcontext.country", aCtx.getResolvedCountry ().getID ());
      aSpan.setStatusOk ();
      return aCtx;
    });
  }

  @NonNull
  private static HybridXmlContext _empty ()
  {
    return new HybridXmlContext (null, null, null, null, null, EZugferdCountry.OTHER);
  }

  @Nullable
  private static Element _child (@Nullable final Element aParent, @NonNull final String sNS, @NonNull final String sLocal)
  {
    return XMLHelper.getFirstChildElementOfName (aParent, sNS, sLocal);
  }

  @Nullable
  private static String _text (@Nullable final Element aElement)
  {
    if (aElement == null)
      return null;
    final String sText = aElement.getTextContent ();
    if (sText == null)
      return null;
    final String sTrimmed = sText.trim ();
    return sTrimmed.length () == 0 ? null : sTrimmed;
  }

  /** @return the leading 2-char country prefix of a VAT identifier, or <code>null</code>. */
  @Nullable
  private static String _vatCountryPrefix (@Nullable final String sVatID)
  {
    if (sVatID == null)
      return null;
    final String sTrimmed = sVatID.trim ();
    return sTrimmed.length () >= 2 ? sTrimmed.substring (0, 2) : null;
  }

  /** @return the resolved country using the PEPPOL VAT-prefix-first chain. */
  @NonNull
  private static EZugferdCountry _resolve (@Nullable final String sSellerVat,
                                           @Nullable final String sTaxRepVat,
                                           @Nullable final String sPostal)
  {
    if (sSellerVat != null)
      return EZugferdCountry.getFromCountryCode (sSellerVat);
    if (sTaxRepVat != null)
      return EZugferdCountry.getFromCountryCode (sTaxRepVat);
    return EZugferdCountry.getFromCountryCode (sPostal);
  }

  @NonNull
  private static HybridXmlContext _fromCII (@NonNull final Element eRoot)
  {
    String sCustomizationID = null;
    String sBusinessProcess = null;
    final Element eContext = _child (eRoot, CII_NS_RSM, "ExchangedDocumentContext");
    if (eContext != null)
    {
      final Element eGuideline = _child (eContext, CII_NS_RAM, "GuidelineSpecifiedDocumentContextParameter");
      sCustomizationID = _text (_child (eGuideline, CII_NS_RAM, "ID"));
      final Element eProcess = _child (eContext, CII_NS_RAM, "BusinessProcessSpecifiedDocumentContextParameter");
      sBusinessProcess = _text (_child (eProcess, CII_NS_RAM, "ID"));
    }

    final Element eTx = _child (eRoot, CII_NS_RSM, "SupplyChainTradeTransaction");
    final Element eAgreement = _child (eTx, CII_NS_RAM, "ApplicableHeaderTradeAgreement");

    // Seller VAT (schemeID='VA') + postal country
    final Element eSeller = _child (eAgreement, CII_NS_RAM, "SellerTradeParty");
    final String sSellerVat = _vatCountryPrefix (_ciiVatID (eSeller));
    final Element eAddr = _child (eSeller, CII_NS_RAM, "PostalTradeAddress");
    final String sPostal = _text (_child (eAddr, CII_NS_RAM, "CountryID"));

    // Tax representative VAT
    final Element eTaxRep = _child (eAgreement, CII_NS_RAM, "SellerTaxRepresentativeTradeParty");
    final String sTaxRepVat = _vatCountryPrefix (_ciiVatID (eTaxRep));

    return new HybridXmlContext ("CII",
                                 sCustomizationID,
                                 sBusinessProcess,
                                 sSellerVat,
                                 sPostal,
                                 _resolve (sSellerVat, sTaxRepVat, sPostal));
  }

  /** Extract the VAT registration ID (schemeID='VA') from a CII trade party. */
  @Nullable
  private static String _ciiVatID (@Nullable final Element aParty)
  {
    if (aParty == null)
      return null;
    for (final Element eReg : XMLHelper.getChildElementIteratorNS (aParty, CII_NS_RAM, "SpecifiedTaxRegistration"))
    {
      final Element eID = _child (eReg, CII_NS_RAM, "ID");
      if (eID != null && "VA".equals (eID.getAttribute ("schemeID")))
        return _text (eID);
    }
    return null;
  }

  @NonNull
  private static HybridXmlContext _fromUBL (@NonNull final Element eRoot)
  {
    final String sCustomizationID = _text (_child (eRoot, UBL_NS_CBC, "CustomizationID"));
    final String sBusinessProcess = _text (_child (eRoot, UBL_NS_CBC, "ProfileID"));

    final Element eSupplier = _child (eRoot, UBL_NS_CAC, "AccountingSupplierParty");
    final Element eParty = _child (eSupplier, UBL_NS_CAC, "Party");
    final String sSellerVat = _vatCountryPrefix (_ublVatID (eParty));

    final Element ePostal = _child (eParty, UBL_NS_CAC, "PostalAddress");
    final Element eCountry = _child (ePostal, UBL_NS_CAC, "Country");
    final String sPostal = _text (_child (eCountry, UBL_NS_CBC, "IdentificationCode"));

    final Element eTaxRep = _child (eRoot, UBL_NS_CAC, "TaxRepresentativeParty");
    final String sTaxRepVat = _vatCountryPrefix (_ublVatID (eTaxRep));

    return new HybridXmlContext ("UBL",
                                 sCustomizationID,
                                 sBusinessProcess,
                                 sSellerVat,
                                 sPostal,
                                 _resolve (sSellerVat, sTaxRepVat, sPostal));
  }

  /** Extract the VAT CompanyID (PartyTaxScheme where TaxScheme/ID='VAT') from a UBL party. */
  @Nullable
  private static String _ublVatID (@Nullable final Element aParty)
  {
    if (aParty == null)
      return null;
    for (final Element ePTS : XMLHelper.getChildElementIteratorNS (aParty, UBL_NS_CAC, "PartyTaxScheme"))
    {
      final Element eScheme = _child (ePTS, UBL_NS_CAC, "TaxScheme");
      final String sSchemeID = _text (_child (eScheme, UBL_NS_CBC, "ID"));
      if ("VAT".equals (sSchemeID))
        return _text (_child (ePTS, UBL_NS_CBC, "CompanyID"));
    }
    return null;
  }
}
```

> `_child` is null-tolerant (`XMLHelper.getFirstChildElementOfName` returns `null` for a `null` start
> node), so the chained navigation never NPEs on a missing intermediate element.

### 4.4 Wiring into `HybridValidator` (design level — verify against the full class)

Keep it opt-in via a new setting `HybridValidatorSettings.setReadXmlContext(boolean)` (default `false`),
so the carrier-only default path never touches the payload.

**Reuse the already-open document — do not re-open the PDF.** `HybridValidator.validate` already opens
the document once via `HybridDocument.open (aSource, m_aSettings.getLimits ())` (`HybridValidator.java:106`)
and holds it (`aDoc`) for the whole run. It must **not** call the `IHybridSource` overload of
`readXmlContext`, because that goes back through `HybridExtractor.extractInvoiceXml` and opens/parses the
PDF a second time within a single `validate ()` call. Instead, when `isReadXmlContext ()` is enabled,
pull the invoice XML bytes from the open `aDoc` once (the same way `HybridExtractor.extractInvoiceXml`
does — iterate `aDoc.listAttachments ()` for the `isInvoiceXml ()` entry) and pass those bytes to the
byte-array overload `HybridXmlContextReader.readXmlContext (byte[])`. That parses the XML exactly once
and never re-opens the PDF.

- **BR-HYBRID-15:** when enabled, read `HybridXmlContext.getCustomizationID()`, derive the profile tier
  from it, and compare against the resolved `fx:ConformanceLevel`; emit `WARNING` on mismatch instead of
  the current `INFORMATION` placeholder.
- **Auto country:** in `HybridValidator.validate`, if `getSettings().getCountry()` is still the default
  `OTHER`, fall back to the context's `getResolvedCountry()` so BR-HYBRID-DE-* / FR-* fire without the
  caller having to know the jurisdiction up front. Gate this on the same `isReadXmlContext ()` flag, so
  the carrier-only default path is unaffected.

> **Not addressed here: ZUGFeRD 2.5 flavor disambiguation (motivation §1.2).** BT-24 is what
> distinguishes ZUGFeRD 2.5 from 2.3/2.4 at the XMP-identical level, and `HybridXmlContext` now surfaces
> it — but this design deliberately does **not** feed BT-24 back into `EZugferdFlavor` detection. Flavor
> detection stays carrier-only (XMP) and unchanged; consuming the surfaced `getCustomizationID ()` to
> refine the flavor is a possible follow-up, called out as future work rather than delivered here. The §1
> item is therefore motivation/context, not a requirement satisfied by this proposal.

## 5. Verified ph-commons APIs (no need to re-verify)

Checked against local sources under `~/dev/git/ph-commons`:

- `DOMReader.readXMLDOM (byte[] aXML, IDOMReaderSettings aSettings)` — exists (`DOMReader.java:462`).
- `DOMReaderSettings.setFeatureValue (EXMLParserFeature, boolean)` — fluent, returns `DOMReaderSettings`.
- `EXMLParserFeature` has `SECURE_PROCESSING`, `DISALLOW_DOCTYPE_DECL`, `EXTERNAL_GENERAL_ENTITIES`,
  `EXTERNAL_PARAMETER_ENTITIES`. Note: `DOMReaderDefaultSettings` does **not** disable DTDs by default, so
  the explicit hardening above is required for the untrusted payload.
- DOM is **namespace-aware by default** (`XMLFactory.DEFAULT_DOM_NAMESPACE_AWARE = true`), so
  `getLocalName()` / namespace-based navigation work.
- `XMLHelper.getFirstChildElementOfName (Node, String nsURI, String localName)` — null-tolerant 3-arg
  namespace+localName overload; exists (`XMLHelper.java:402`).
- `XMLHelper.getChildElementIteratorNS (Node, String nsURI, String localName)` — used for the
  multi-occurrence `PartyTaxScheme` / `SpecifiedTaxRegistration` loops; exists (`XMLHelper.java:729`).
  Note the **`NS` suffix**: the plain `getChildElementIterator` only has `(Node)` and `(Node, sTagName)`
  overloads — there is **no** 3-arg `getChildElementIterator (Node, nsURI, localName)`, so the
  namespace-aware loops must call `getChildElementIteratorNS`.

## 6. Open questions

### 6a. Decisions required (cannot be resolved from code — pick before implementing)

1. **The scope boundary itself.** CLAUDE.md declares XML business-rule validation out of scope, and the
   architecture keeps XML-payload access out of every package except `pdfbox` (which only touches PDF
   internals, never parses the XML). This reader is the first code to parse the invoice XML. Identifier-
   only extraction stays on the right side of the "no rule validation" line, but it *does* cross the
   "nothing parses the payload" line. Off-by-default + separate class is the intended containment — but
   the boundary shift must be confirmed before anything is built. (See §2 "scope caveat".)
2. **Placement of `readXmlContext`** — this proposal puts it in a new `HybridXmlContextReader` in the
   `inspect` package (keeps carrier-only `HybridInspector` clean). Alternative: fold static methods into
   `HybridInspector`. *Recommendation: new class.* Pick one.
3. **ZUGFeRD 1.0** — legacy `CrossIndustryDocument` has a different root + context path; currently out of
   scope. Decide whether to add a third branch.

### 6b. Gaps in the design that need a decision (found while verifying against source)

4. **No BT-24 → profile-tier mapping exists.** BR-HYBRID-15's wiring (§4.4) says "derive the profile
   tier from `getCustomizationID ()` and compare against `fx:ConformanceLevel`", but
   `EZugferdProfile.getFromIDOrNull` (`EZugferdProfile.java:74`) only maps the *XMP ConformanceLevel
   value* ("BASIC", "EN 16931") — **not** BT-24 URNs like
   `urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic`. Those URNs vary by profile *and* by
   spec version, so BR-HYBRID-15 needs a **new** URN→`EZugferdProfile` parser that does not exist yet.
   Decide: build that mapping now (and where — a new static on `EZugferdProfile`?), or ship only the
   auto-country half in this pass and leave BR-HYBRID-15 as the current `INFORMATION` placeholder.
5. **Auto-country silently couples to the DE PDF/A downgrade.** If auto-detection resolves the country to
   `DE`, it does not just enable BR-HYBRID-DE-*; it also triggers the DE PDF/A-3 error→warning downgrade
   (`isApplyDePdfADowngrade`, default `true`; `HybridValidator.java:133,320`). So turning on
   `setReadXmlContext (true)` for a German invoice would auto-relax PDF/A severity — behaviour that today
   requires an explicit `setCountry (DE)`. Decide: is that acceptable, or should auto-detected DE gate
   the country rule layers **but not** the PDF/A downgrade?

### 6c. Verify against real sample files (implementation-time confirmation)

6. **CII VAT schemeID** — confirm the seller VAT registration is `ram:SpecifiedTaxRegistration/ram:ID`
   with `@schemeID='VA'` in real Factur-X/ZUGFeRD 2.x files (vs. `'VAT'` or absent).
7. **CII tax representative element name** — confirm `ram:SellerTaxRepresentativeTradeParty` under
   `ram:ApplicableHeaderTradeAgreement`.
8. **UBL VAT** — confirm PEPPOL/XRechnung UBL uses `PartyTaxScheme[TaxScheme/ID='VAT']/CompanyID` for the
   seller VAT (this matches the PEPPOL Schematron `supplierCountry` derivation).

### 6d. Resolved

9. **`XMLHelper` namespace iterator** — use `getChildElementIteratorNS (Node, nsURI, localName)`
   (`XMLHelper.java:729`); the plain `getChildElementIterator` has no 3-arg namespace+localName overload.
   The code sketch above already uses the `NS` variant.
10. **Double PDF open in the validator** — resolved via the byte-array overload
    `readXmlContext (byte[])`; `HybridValidator` reuses its already-open document instead of re-opening
    through the `IHybridSource` path. See §4.4.

## 7. Testing

- Add `HybridXmlContextReaderTest` driven by the `kaltblut-testfiles` samples (one representative PDF per
  generation, reuse the `KaltblutTestFiles.*` constants — do not ship new PDFs).
- Assert: CII BT-24 extraction, UBL BT-24 extraction, seller-country resolution via VAT prefix,
  fallback to postal country when no VAT ID, `OTHER` when neither present, and a non-hybrid / no-XML
  input yielding the empty context.

## 8. Don't forget

- Update the README **News and noteworthy** ("work in progress" entry for the upcoming version) once
  implemented.
- Update `CLAUDE.md` (core architecture / package map) to mention the new opt-in XML-context capability
  and its scope boundary.
