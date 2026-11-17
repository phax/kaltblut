/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.kaltblut.core.inspect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.codec.RFC5234Helper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.HybridAttachment;
import com.helger.kaltblut.core.model.HybridXmlContext;
import com.helger.kaltblut.core.pdfbox.HybridDocument;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.telemetry.ETelemetrySpanKind;
import com.helger.telemetry.ITelemetrySpan;
import com.helger.telemetry.Telemetry;
import com.helger.xml.EXMLParserFeature;
import com.helger.xml.XMLHelper;
import com.helger.xml.sax.DoNothingSAXErrorHandler;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.read.DOMReaderSettings;

/**
 * Optional reader that peeks into the embedded invoice XML for the few routing-relevant context
 * fields (BT-24, BT-23, Seller country). This is the one place in Kaltblut that looks at the XML
 * payload; it reads identifiers only and performs no business-rule validation.
 * <p>
 * Targets modern CII (ZUGFeRD 2.x / Factur-X, root <code>rsm:CrossIndustryInvoice</code>) and UBL
 * (<code>Invoice</code> / <code>CreditNote</code>). ZUGFeRD 1.0's legacy
 * <code>CrossIndustryDocument</code> structure is not covered and yields
 * {@link HybridXmlContext#EMPTY}.
 * <p>
 * The seller and buyer countries are resolved like the Peppol <code>supplierCountry</code> /
 * <code>customerCountry</code> derivations: the VAT identifier prefix first (for the seller then
 * the tax representative VAT identifier prefix), then the postal country (BT-40 / BT-55).
 *
 * @author Alexandre Gabriel Carvalho Silva
 */
@Immutable
public final class HybridXmlContextReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HybridXmlContextReader.class);

  private static final String CII_NS_RSM = "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100";
  private static final String CII_NS_RAM = "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100";
  private static final String UBL_NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
  // UBL 2.1 to 2.5 share the major-version namespaces
  private static final String UBL_NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
  private static final String UBL_NS_CREDITNOTE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
  private static final String UBL_NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";

  /** 2-letter VAT identifier prefixes that do not denote a country. */
  private static final ICommonsSet <String> NON_COUNTRY_VAT_PREFIXES = new CommonsHashSet <> ("EU", "IM");

  /** Upper bound for identifier values copied into span attributes. */
  private static final int MAX_SPAN_ATTR_LENGTH = 256;

  // Untrusted payload: harden against XXE / DTD / entity expansion. DOMReaderDefaultSettings does
  // not disable DTDs by default, so this must be explicit.
  private static final DOMReaderSettings XML_SETTINGS = new DOMReaderSettings ();

  static
  {
    XML_SETTINGS.setFeatureValue (EXMLParserFeature.SECURE_PROCESSING, true)
                .setFeatureValue (EXMLParserFeature.DISALLOW_DOCTYPE_DECL, true)
                .setFeatureValue (EXMLParserFeature.EXTERNAL_GENERAL_ENTITIES, false)
                .setFeatureValue (EXMLParserFeature.EXTERNAL_PARAMETER_ENTITIES, false)
                .setFeatureValue (EXMLParserFeature.LOAD_EXTERNAL_DTD, false)
                // Malformed XML is an expected, handled case: log it once below, not here
                .setErrorHandler (new DoNothingSAXErrorHandler ());
    XML_SETTINGS.exceptionCallbacks ()
                .set (ex -> LOGGER.debug ("Embedded invoice XML is not parsable: " + ex.getMessage ()));
  }

  private HybridXmlContextReader ()
  {}

  /**
   * Read the routing context from the embedded invoice XML, using {@link HybridLimits#DEFAULTS}.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the context; never <code>null</code>.
   * @throws IOException
   *         on I/O / PDF / XMP failure or default-limit violation.
   * @see #readXmlContext(IHybridSource, HybridLimits)
   */
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
   * @return the context; never <code>null</code>. When no invoice XML is present or the syntax is
   *         unrecognised, {@link HybridXmlContext#EMPTY} is returned.
   * @throws IOException
   *         on I/O / PDF / XMP failure or limit violation.
   */
  @NonNull
  public static HybridXmlContext readXmlContext (@NonNull final IHybridSource aSource,
                                                 @NonNull final HybridLimits aLimits) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    return Telemetry.withSpanThrowing ("kaltblut.xmlcontext", ETelemetrySpanKind.INTERNAL, aSpan -> {
      final HybridXmlContext aCtx = HybridDocument.withOpenDocument (aSource, aLimits, aDoc -> {
        for (final HybridAttachment aAtt : aDoc.listAttachments ())
          if (aAtt.isInvoiceXml () && aAtt.getSize () > 0)
            return _parse (aAtt.getInputStream ());
        return HybridXmlContext.EMPTY;
      });
      _setSpanAttributes (aSpan, aCtx);
      return aCtx;
    });
  }

  /**
   * Read the routing context from already-extracted invoice XML bytes. Callers that have already
   * opened the PDF should use this overload with the bytes they already extracted, to avoid opening
   * and parsing the PDF a second time.
   *
   * @param aXml
   *        the raw invoice XML bytes, or <code>null</code> if the PDF carries no invoice XML.
   * @return the context; never <code>null</code>. When <code>aXml</code> is <code>null</code>,
   *         cannot be parsed, or the syntax is unrecognised, {@link HybridXmlContext#EMPTY} is
   *         returned.
   */
  @NonNull
  public static HybridXmlContext readXmlContext (@Nullable final byte [] aXml)
  {
    return _readXmlContext (aXml == null ? null : new NonBlockingByteArrayInputStream (aXml));
  }

  /**
   * Read the routing context from an already-extracted invoice XML attachment, without copying its
   * content.
   *
   * @param aInvoiceXml
   *        the invoice XML attachment. May not be <code>null</code>.
   * @return the context; never <code>null</code>. When the XML cannot be parsed or the syntax is
   *         unrecognised, {@link HybridXmlContext#EMPTY} is returned.
   */
  @NonNull
  public static HybridXmlContext readXmlContext (@NonNull final HybridAttachment aInvoiceXml)
  {
    ValueEnforcer.notNull (aInvoiceXml, "InvoiceXml");
    return _readXmlContext (aInvoiceXml.getInputStream ());
  }

  @NonNull
  private static HybridXmlContext _readXmlContext (@Nullable final InputStream aXmlIS)
  {
    return Telemetry.withSpanThrowing ("kaltblut.xmlcontext", ETelemetrySpanKind.INTERNAL, aSpan -> {
      final HybridXmlContext aCtx = aXmlIS == null ? HybridXmlContext.EMPTY : _parse (aXmlIS);
      _setSpanAttributes (aSpan, aCtx);
      return aCtx;
    });
  }

  private static void _setSpanAttributes (@NonNull final ITelemetrySpan aSpan, @NonNull final HybridXmlContext aCtx)
  {
    aSpan.setAttribute ("kaltblut.xmlcontext.syntax", aCtx.getSyntax () != null ? aCtx.getSyntax () : "none");
    final String sCustomizationID = aCtx.getCustomizationID ();
    if (sCustomizationID != null)
      aSpan.setAttribute ("kaltblut.xmlcontext.customizationid",
                          StringHelper.getCutAfterLength (sCustomizationID, MAX_SPAN_ATTR_LENGTH));
    aSpan.setAttribute ("kaltblut.xmlcontext.country", aCtx.getResolvedCountry ().getID ());
    aSpan.setStatusOk ();
  }

  @NonNull
  private static HybridXmlContext _parse (@NonNull final InputStream aXmlIS)
  {
    final Document aDoc = DOMReader.readXMLDOM (aXmlIS, XML_SETTINGS);
    if (aDoc == null)
    {
      LOGGER.warn ("Failed to parse the embedded invoice XML for context extraction");
      return HybridXmlContext.EMPTY;
    }
    final Element eRoot = aDoc.getDocumentElement ();
    final String sNS = eRoot == null ? null : eRoot.getNamespaceURI ();
    final String sLocal = eRoot == null ? null : eRoot.getLocalName ();
    if (CII_NS_RSM.equals (sNS) && "CrossIndustryInvoice".equals (sLocal))
      return _fromCII (eRoot);
    if ((UBL_NS_INVOICE.equals (sNS) && "Invoice".equals (sLocal)) ||
        (UBL_NS_CREDITNOTE.equals (sNS) && "CreditNote".equals (sLocal)))
      return _fromUBL (eRoot);
    return HybridXmlContext.EMPTY;
  }

  @Nullable
  private static Element _child (@Nullable final Element aParent,
                                 @NonNull final String sNamespaceURI,
                                 @NonNull final String sLocalName)
  {
    return aParent == null ? null : XMLHelper.getFirstChildElementOfName (aParent, sNamespaceURI, sLocalName);
  }

  @Nullable
  private static String _text (@Nullable final Element aElement)
  {
    if (aElement == null)
      return null;
    final String sTrimmed = StringHelper.trim (aElement.getTextContent ());
    return StringHelper.isNotEmpty (sTrimmed) ? sTrimmed : null;
  }

  /**
   * @return the leading 2-letter country prefix of a VAT identifier, or <code>null</code> if there
   *         is none (e.g. a VAT number without prefix), so that resolution falls through to the
   *         next source.
   */
  @Nullable
  private static String _vatCountryPrefix (@Nullable final String sVatID)
  {
    // sVatID is already trimmed by _text
    if (sVatID == null || sVatID.length () < 2)
      return null;
    if (!RFC5234Helper.isAlpha (sVatID.charAt (0)) || !RFC5234Helper.isAlpha (sVatID.charAt (1)))
      return null;
    final String sPrefix = _upper (StringHelper.getCutAfterLength (sVatID, 2));
    // One-Stop-Shop identifiers (non-Union scheme "EU", import scheme "IM") carry no country
    return NON_COUNTRY_VAT_PREFIXES.contains (sPrefix) ? null : sPrefix;
  }

  @Nullable
  private static String _upper (@Nullable final String s)
  {
    return s == null ? null : s.toUpperCase (Locale.ROOT);
  }

  /**
   * @return the resolved country using the Peppol VAT-prefix-first chain. The tax representative is
   *         only used for the seller.
   */
  @NonNull
  private static EZugferdCountry _resolve (@Nullable final String sSellerVatCountry,
                                           @Nullable final String sTaxRepVatCountry,
                                           @Nullable final String sPostalCountry)
  {
    if (sSellerVatCountry != null)
      return EZugferdCountry.getFromCountryCode (sSellerVatCountry);
    if (sTaxRepVatCountry != null)
      return EZugferdCountry.getFromCountryCode (sTaxRepVatCountry);
    return EZugferdCountry.getFromCountryCode (sPostalCountry);
  }

  @NonNull
  private static HybridXmlContext _build (@NonNull final String sSyntax,
                                          @Nullable final String sCustomizationID,
                                          @Nullable final String sBusinessProcess,
                                          @Nullable final String sSellerVatID,
                                          @Nullable final String sSellerPostalCountry,
                                          @Nullable final String sTaxRepVatID,
                                          @Nullable final String sBuyerVatID,
                                          @Nullable final String sBuyerPostalCountry)
  {
    final String sSellerVatCountry = _vatCountryPrefix (sSellerVatID);
    final String sBuyerVatCountry = _vatCountryPrefix (sBuyerVatID);
    return new HybridXmlContext (sSyntax,
                                 sCustomizationID,
                                 sBusinessProcess,
                                 sSellerVatCountry,
                                 sSellerPostalCountry,
                                 _resolve (sSellerVatCountry, _vatCountryPrefix (sTaxRepVatID), sSellerPostalCountry),
                                 sBuyerVatCountry,
                                 sBuyerPostalCountry,
                                 _resolve (sBuyerVatCountry, null, sBuyerPostalCountry));
  }

  @NonNull
  private static HybridXmlContext _fromCII (@NonNull final Element eRoot)
  {
    final Element eContext = _child (eRoot, CII_NS_RSM, "ExchangedDocumentContext");
    final Element eGuideline = _child (eContext, CII_NS_RAM, "GuidelineSpecifiedDocumentContextParameter");
    final String sCustomizationID = _text (_child (eGuideline, CII_NS_RAM, "ID"));
    final Element eProcess = _child (eContext, CII_NS_RAM, "BusinessProcessSpecifiedDocumentContextParameter");
    final String sBusinessProcess = _text (_child (eProcess, CII_NS_RAM, "ID"));

    final Element eTx = _child (eRoot, CII_NS_RSM, "SupplyChainTradeTransaction");
    final Element eAgreement = _child (eTx, CII_NS_RAM, "ApplicableHeaderTradeAgreement");

    // VAT (schemeID='VA') + postal country
    final Element eSeller = _child (eAgreement, CII_NS_RAM, "SellerTradeParty");
    final Element eTaxRep = _child (eAgreement, CII_NS_RAM, "SellerTaxRepresentativeTradeParty");
    final Element eBuyer = _child (eAgreement, CII_NS_RAM, "BuyerTradeParty");
    return _build (HybridXmlContext.SYNTAX_CII,
                   sCustomizationID,
                   sBusinessProcess,
                   _ciiVatID (eSeller),
                   _ciiPostalCountry (eSeller),
                   _ciiVatID (eTaxRep),
                   _ciiVatID (eBuyer),
                   _ciiPostalCountry (eBuyer));
  }

  @Nullable
  private static String _ciiPostalCountry (@Nullable final Element aParty)
  {
    final Element eAddress = _child (aParty, CII_NS_RAM, "PostalTradeAddress");
    return _upper (_text (_child (eAddress, CII_NS_RAM, "CountryID")));
  }

  /**
   * @return the VAT registration ID (<code>schemeID='VA'</code>) of a CII trade party, or
   *         <code>null</code>.
   */
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

    final Element eSeller = _child (_child (eRoot, UBL_NS_CAC, "AccountingSupplierParty"), UBL_NS_CAC, "Party");
    final Element eTaxRep = _child (eRoot, UBL_NS_CAC, "TaxRepresentativeParty");
    final Element eBuyer = _child (_child (eRoot, UBL_NS_CAC, "AccountingCustomerParty"), UBL_NS_CAC, "Party");
    return _build (HybridXmlContext.SYNTAX_UBL,
                   sCustomizationID,
                   sBusinessProcess,
                   _ublVatID (eSeller),
                   _ublPostalCountry (eSeller),
                   _ublVatID (eTaxRep),
                   _ublVatID (eBuyer),
                   _ublPostalCountry (eBuyer));
  }

  @Nullable
  private static String _ublPostalCountry (@Nullable final Element aParty)
  {
    final Element eAddress = _child (aParty, UBL_NS_CAC, "PostalAddress");
    final Element eCountry = _child (eAddress, UBL_NS_CAC, "Country");
    return _upper (_text (_child (eCountry, UBL_NS_CBC, "IdentificationCode")));
  }

  /**
   * @return the VAT CompanyID (<code>PartyTaxScheme</code> where <code>TaxScheme/ID='VAT'</code>)
   *         of a UBL party, or <code>null</code>.
   */
  @Nullable
  private static String _ublVatID (@Nullable final Element aParty)
  {
    if (aParty == null)
      return null;
    for (final Element ePTS : XMLHelper.getChildElementIteratorNS (aParty, UBL_NS_CAC, "PartyTaxScheme"))
    {
      final Element eScheme = _child (ePTS, UBL_NS_CAC, "TaxScheme");
      if ("VAT".equals (_text (_child (eScheme, UBL_NS_CBC, "ID"))))
        return _text (_child (ePTS, UBL_NS_CBC, "CompanyID"));
    }
    return null;
  }
}
