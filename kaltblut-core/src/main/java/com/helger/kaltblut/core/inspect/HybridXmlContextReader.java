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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.codec.RFC5234Helper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
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

/**
 * Optional reader that peeks into the embedded invoice XML for the few routing-relevant context
 * fields (BT-24, BT-23, seller and buyer country). This is the one place in Kaltblut that looks at
 * the XML payload; it reads identifiers only and performs no business-rule validation.
 * <p>
 * Targets modern CII (ZUGFeRD 2.x / Factur-X, root <code>rsm:CrossIndustryInvoice</code>) and UBL
 * (<code>Invoice</code> / <code>CreditNote</code>). ZUGFeRD 1.0's legacy
 * <code>CrossIndustryDocument</code> structure is not covered and yields
 * {@link HybridXmlContext#EMPTY}.
 * <p>
 * The seller and buyer countries are resolved like the Peppol <code>supplierCountry</code> /
 * <code>customerCountry</code> derivations: the VAT identifier prefix first (for the seller then
 * the tax representative VAT identifier prefix), then the postal country (BT-40 / BT-55).
 * <p>
 * The XML is read with StAX in a single pass that stops once all parties have been read (after the
 * CII <code>ApplicableHeaderTradeAgreement</code>, or at the first UBL line). Content after that
 * point is not checked for well-formedness.
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

  /**
   * 2-letter VAT identifier prefixes that are no ISO 3166-1 country code of the issuing country, so
   * that the postal country is used instead:
   * <ul>
   * <li>EL - Greece (ISO code GR)</li>
   * <li>XI - Northern Ireland under the Windsor Framework (ISO user-assigned range)</li>
   * <li>EU - non-Union OSS scheme</li>
   * <li>IM - IOSS identification number (not the Isle of Man)</li>
   * <li>IN - IOSS intermediary (not India)</li>
   * </ul>
   */
  private static final ICommonsSet <String> NON_COUNTRY_VAT_PREFIXES = new CommonsHashSet <> ("EL",
                                                                                              "XI",
                                                                                              "EU",
                                                                                              "IM",
                                                                                              "IN");

  /** Upper bound for identifier values copied into span attributes. */
  private static final int MAX_SPAN_ATTR_LENGTH = 256;

  private static final int PARTY_SELLER = 0;
  private static final int PARTY_TAX_REP = 1;
  private static final int PARTY_BUYER = 2;

  // Paths are relative to the root element; segments are "<ns-token>:<local name>"
  private static final String CII_GUIDELINE_ID = "rsm:ExchangedDocumentContext/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID";
  private static final String CII_PROCESS_ID = "rsm:ExchangedDocumentContext/ram:BusinessProcessSpecifiedDocumentContextParameter/ram:ID";
  private static final String CII_AGREEMENT = "rsm:SupplyChainTradeTransaction/ram:ApplicableHeaderTradeAgreement";
  private static final String [] CII_PARTIES = { CII_AGREEMENT + "/ram:SellerTradeParty",
                                                 CII_AGREEMENT + "/ram:SellerTaxRepresentativeTradeParty",
                                                 CII_AGREEMENT + "/ram:BuyerTradeParty" };
  private static final String [] CII_PARTY_PREFIXES = _withSlash (CII_PARTIES);
  private static final String CII_VAT_ID = "/ram:SpecifiedTaxRegistration/ram:ID";
  private static final String CII_POSTAL_COUNTRY = "/ram:PostalTradeAddress/ram:CountryID";

  private static final String UBL_CUSTOMIZATION_ID = "cbc:CustomizationID";
  private static final String UBL_PROFILE_ID = "cbc:ProfileID";
  private static final String [] UBL_PARTIES = { "cac:AccountingSupplierParty/cac:Party",
                                                 "cac:TaxRepresentativeParty",
                                                 "cac:AccountingCustomerParty/cac:Party" };
  private static final String [] UBL_PARTY_PREFIXES = _withSlash (UBL_PARTIES);
  private static final String UBL_TAX_SCHEME = "/cac:PartyTaxScheme";
  private static final String UBL_COMPANY_ID = UBL_TAX_SCHEME + "/cbc:CompanyID";
  private static final String UBL_TAX_SCHEME_ID = UBL_TAX_SCHEME + "/cac:TaxScheme/cbc:ID";
  private static final String UBL_POSTAL_COUNTRY = "/cac:PostalAddress/cac:Country/cbc:IdentificationCode";

  /** Elements that may repeat: every occurrence is inspected, not only the first one. */
  private static final ICommonsSet <String> REPEATABLE = new CommonsHashSet <> ("ram:SpecifiedTaxRegistration",
                                                                                "cac:PartyTaxScheme");

  // Sets, because every visited element is looked up by its path
  private static final ICommonsSet <String> CII_LEAVES = new CommonsHashSet <> ();
  private static final ICommonsSet <String> CII_BRANCHES = new CommonsHashSet <> ();
  private static final ICommonsSet <String> UBL_LEAVES = new CommonsHashSet <> ();
  private static final ICommonsSet <String> UBL_BRANCHES = new CommonsHashSet <> ();

  static
  {
    CII_LEAVES.addAll (CII_GUIDELINE_ID, CII_PROCESS_ID);
    for (final String sParty : CII_PARTIES)
      CII_LEAVES.addAll (sParty + CII_VAT_ID, sParty + CII_POSTAL_COUNTRY);
    for (final String sLeaf : CII_LEAVES)
      _addBranches (sLeaf, CII_BRANCHES);

    UBL_LEAVES.addAll (UBL_CUSTOMIZATION_ID, UBL_PROFILE_ID);
    for (final String sParty : UBL_PARTIES)
      UBL_LEAVES.addAll (sParty + UBL_COMPANY_ID, sParty + UBL_TAX_SCHEME_ID, sParty + UBL_POSTAL_COUNTRY);
    for (final String sLeaf : UBL_LEAVES)
      _addBranches (sLeaf, UBL_BRANCHES);
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
   * opened the PDF should use this overload or {@link #readXmlContext(HybridAttachment)} (which
   * avoids a copy), to avoid opening and parsing the PDF a second time.
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
    return Telemetry.withSpan ("kaltblut.xmlcontext", ETelemetrySpanKind.INTERNAL, aSpan -> {
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
    aSpan.setAttribute ("kaltblut.xmlcontext.country", aCtx.getResolvedSellerCountry ().getID ());
    aSpan.setStatusOk ();
  }

  /**
   * @return a new, hardened StAX factory. Created per parse, because the StAX specification does
   *         not guarantee that a factory is thread-safe.
   */
  @NonNull
  private static XMLInputFactory _createInputFactory ()
  {
    // Untrusted payload: no DTDs (a DOCTYPE is rejected in _parse) and no external entities. The
    // JDK default implementation is used, independent of any StAX provider on the classpath.
    final XMLInputFactory aFactory = XMLInputFactory.newDefaultFactory ();
    aFactory.setProperty (XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
    aFactory.setProperty (XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    aFactory.setProperty (XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    return aFactory;
  }

  @NonNull
  private static HybridXmlContext _parse (@NonNull final InputStream aXmlIS)
  {
    XMLStreamReader aReader = null;
    try
    {
      aReader = _createInputFactory ().createXMLStreamReader (aXmlIS);
      while (aReader.hasNext ())
      {
        final int nEvent = aReader.next ();
        if (nEvent == XMLStreamConstants.DTD)
        {
          LOGGER.warn ("The embedded invoice XML contains a DOCTYPE, which is not allowed");
          return HybridXmlContext.EMPTY;
        }
        if (nEvent == XMLStreamConstants.START_ELEMENT)
        {
          final String sNS = aReader.getNamespaceURI ();
          final String sLocal = aReader.getLocalName ();
          if (CII_NS_RSM.equals (sNS) && "CrossIndustryInvoice".equals (sLocal))
            return new ContextCollector (true).collect (aReader);
          if ((UBL_NS_INVOICE.equals (sNS) && "Invoice".equals (sLocal)) ||
              (UBL_NS_CREDITNOTE.equals (sNS) && "CreditNote".equals (sLocal)))
            return new ContextCollector (false).collect (aReader);
          return HybridXmlContext.EMPTY;
        }
      }
      return HybridXmlContext.EMPTY;
    }
    catch (final XMLStreamException | RuntimeException ex)
    {
      // RuntimeException: the DOM based predecessor treated every parser failure as unparsable
      LOGGER.warn ("Failed to parse the embedded invoice XML for context extraction");
      LOGGER.debug ("Embedded invoice XML is not parsable: " + ex.getMessage ());
      return HybridXmlContext.EMPTY;
    }
    finally
    {
      if (aReader != null)
        try
        {
          aReader.close ();
        }
        catch (final XMLStreamException ex)
        {
          // ignore
        }
    }
  }

  /**
   * @return the leading 2-letter country prefix of a VAT identifier, or <code>null</code> if there
   *         is none (e.g. a VAT number without prefix), so that resolution falls through to the
   *         next source.
   */
  @Nullable
  private static String _vatCountryPrefix (@Nullable final String sVatID)
  {
    // sVatID is already trimmed by _readText
    if (sVatID == null || sVatID.length () < 2)
      return null;
    if (!RFC5234Helper.isAlpha (sVatID.charAt (0)) || !RFC5234Helper.isAlpha (sVatID.charAt (1)))
      return null;
    final String sPrefix = _upper (StringHelper.getCutAfterLength (sVatID, 2));
    return NON_COUNTRY_VAT_PREFIXES.contains (sPrefix) ? null : sPrefix;
  }

  @Nullable
  private static String _upper (@Nullable final String sValue)
  {
    return sValue == null ? null : sValue.toUpperCase (Locale.ROOT);
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
    return HybridXmlContext.builder ()
                           .syntax (sSyntax)
                           .customizationID (sCustomizationID)
                           .businessProcessType (sBusinessProcess)
                           .sellerVatCountryCode (sSellerVatCountry)
                           .sellerPostalCountryCode (sSellerPostalCountry)
                           .resolvedSellerCountry (_resolve (sSellerVatCountry,
                                                             _vatCountryPrefix (sTaxRepVatID),
                                                             sSellerPostalCountry))
                           .buyerVatCountryCode (sBuyerVatCountry)
                           .buyerPostalCountryCode (sBuyerPostalCountry)
                           .resolvedBuyerCountry (_resolve (sBuyerVatCountry, null, sBuyerPostalCountry))
                           .build ();
  }

  @NonNull
  private static String [] _withSlash (@NonNull final String [] aPaths)
  {
    final String [] ret = new String [aPaths.length];
    for (int i = 0; i < aPaths.length; ++i)
      ret[i] = aPaths[i] + '/';
    return ret;
  }

  private static void _addBranches (@NonNull final String sLeaf, @NonNull final ICommonsSet <String> aTarget)
  {
    int nSlash = sLeaf.indexOf ('/');
    while (nSlash > 0)
    {
      aTarget.add (sLeaf.substring (0, nSlash));
      nSlash = sLeaf.indexOf ('/', nSlash + 1);
    }
  }

  @NonNull
  private static String _token (@Nullable final String sNamespaceURI)
  {
    if (CII_NS_RAM.equals (sNamespaceURI))
      return "ram:";
    if (CII_NS_RSM.equals (sNamespaceURI))
      return "rsm:";
    if (UBL_NS_CBC.equals (sNamespaceURI))
      return "cbc:";
    if (UBL_NS_CAC.equals (sNamespaceURI))
      return "cac:";
    return "?:";
  }

  /** Skip the current element including all its descendants. */
  private static void _skipElement (@NonNull final XMLStreamReader aReader) throws XMLStreamException
  {
    int nDepth = 1;
    while (nDepth > 0)
    {
      final int nEvent = aReader.next ();
      if (nEvent == XMLStreamConstants.END_DOCUMENT)
        throw new XMLStreamException ("Unexpected end of document");
      if (nEvent == XMLStreamConstants.START_ELEMENT)
        nDepth++;
      else
        if (nEvent == XMLStreamConstants.END_ELEMENT)
          nDepth--;
    }
  }

  /**
   * Read the trimmed text content of the current element (including descendants, like DOM
   * <code>getTextContent</code>) and consume its end tag.
   */
  @Nullable
  private static String _readText (@NonNull final XMLStreamReader aReader) throws XMLStreamException
  {
    final StringBuilder aSB = new StringBuilder ();
    int nDepth = 1;
    while (nDepth > 0)
    {
      final int nEvent = aReader.next ();
      switch (nEvent)
      {
        case XMLStreamConstants.END_DOCUMENT:
          throw new XMLStreamException ("Unexpected end of document");
        case XMLStreamConstants.START_ELEMENT:
          nDepth++;
          break;
        case XMLStreamConstants.END_ELEMENT:
          nDepth--;
          break;
        case XMLStreamConstants.CHARACTERS:
        case XMLStreamConstants.CDATA:
        case XMLStreamConstants.SPACE:
          aSB.append (aReader.getText ());
          break;
        default:
          break;
      }
    }
    final String sTrimmed = StringHelper.trim (aSB.toString ());
    return StringHelper.isNotEmpty (sTrimmed) ? sTrimmed : null;
  }

  /** One open element on the path that is being followed. */
  private static final class Frame
  {
    private final String m_sPath;
    private final ICommonsSet <String> m_aSeenChildren = new CommonsHashSet <> ();

    Frame (@NonNull final String sPath)
    {
      m_sPath = sPath;
    }
  }

  /**
   * Single-pass StAX collector. It follows only the paths that lead to a context value, mirroring
   * the "first child element of that name" semantics, skips all other subtrees, and stops as soon
   * as all parties have been read: after the CII <code>ApplicableHeaderTradeAgreement</code>, or at
   * the first UBL invoice / credit note line.
   */
  private static final class ContextCollector
  {
    private final boolean m_bCII;
    private final String [] m_aPartyPrefixes;
    private final ICommonsSet <String> m_aLeaves;
    private final ICommonsSet <String> m_aBranches;
    private final ICommonsList <Frame> m_aStack = new CommonsArrayList <> ();

    private String m_sCustomizationID;
    private String m_sBusinessProcess;
    private final String [] m_aVatID = new String [3];
    private final boolean [] m_aVatDone = new boolean [3];
    private final String [] m_aPostalCountry = new String [3];
    // UBL: values of the PartyTaxScheme currently being read
    private String m_sCompanyID;
    private String m_sTaxSchemeID;

    ContextCollector (final boolean bCII)
    {
      m_bCII = bCII;
      m_aPartyPrefixes = bCII ? CII_PARTY_PREFIXES : UBL_PARTY_PREFIXES;
      m_aLeaves = bCII ? CII_LEAVES : UBL_LEAVES;
      m_aBranches = bCII ? CII_BRANCHES : UBL_BRANCHES;
    }

    /**
     * @return the index of the party whose path prefix is the longest match for the provided path,
     *         or -1. The longest match wins independent of the order of the prefixes.
     */
    private int _getPartyIndex (@NonNull final String sPath)
    {
      int ret = -1;
      int nBestLength = 0;
      for (int i = 0; i < m_aPartyPrefixes.length; ++i)
      {
        final String sPrefix = m_aPartyPrefixes[i];
        if (sPrefix.length () > nBestLength && sPath.startsWith (sPrefix))
        {
          ret = i;
          nBestLength = sPrefix.length ();
        }
      }
      return ret;
    }

    private void _onLeaf (@NonNull final XMLStreamReader aReader, @NonNull final String sPath) throws XMLStreamException
    {
      if (m_bCII)
      {
        if (sPath.equals (CII_GUIDELINE_ID))
          m_sCustomizationID = _readText (aReader);
        else
          if (sPath.equals (CII_PROCESS_ID))
            m_sBusinessProcess = _readText (aReader);
          else
          {
            final int nParty = _getPartyIndex (sPath);
            if (sPath.endsWith (CII_POSTAL_COUNTRY))
              m_aPostalCountry[nParty] = _upper (_readText (aReader));
            else
              if (!m_aVatDone[nParty] && "VA".equals (aReader.getAttributeValue (null, "schemeID")))
              {
                // The first registration with schemeID 'VA' wins
                m_aVatID[nParty] = _readText (aReader);
                m_aVatDone[nParty] = true;
              }
              else
                _skipElement (aReader);
          }
      }
      else
      {
        if (sPath.equals (UBL_CUSTOMIZATION_ID))
          m_sCustomizationID = _readText (aReader);
        else
          if (sPath.equals (UBL_PROFILE_ID))
            m_sBusinessProcess = _readText (aReader);
          else
          {
            final int nParty = _getPartyIndex (sPath);
            if (sPath.endsWith (UBL_POSTAL_COUNTRY))
              m_aPostalCountry[nParty] = _upper (_readText (aReader));
            else
              if (sPath.endsWith (UBL_COMPANY_ID))
                m_sCompanyID = _readText (aReader);
              else
                m_sTaxSchemeID = _readText (aReader);
          }
      }
    }

    private void _onBranchStart (@NonNull final String sPath)
    {
      if (!m_bCII && sPath.endsWith (UBL_TAX_SCHEME))
      {
        m_sCompanyID = null;
        m_sTaxSchemeID = null;
      }
    }

    private void _onBranchEnd (@NonNull final String sPath)
    {
      if (!m_bCII && sPath.endsWith (UBL_TAX_SCHEME))
      {
        // The first PartyTaxScheme with TaxScheme/ID 'VAT' wins
        final int nParty = _getPartyIndex (sPath);
        if (nParty >= 0 && !m_aVatDone[nParty] && "VAT".equals (m_sTaxSchemeID))
        {
          m_aVatID[nParty] = m_sCompanyID;
          m_aVatDone[nParty] = true;
        }
      }
    }

    @NonNull
    HybridXmlContext collect (@NonNull final XMLStreamReader aReader) throws XMLStreamException
    {
      // The reader is positioned on the root start element
      final Frame aRoot = new Frame ("");
      m_aStack.add (aRoot);
      while (!m_aStack.isEmpty () && aReader.hasNext ())
      {
        final int nEvent = aReader.next ();
        if (nEvent == XMLStreamConstants.START_ELEMENT)
        {
          final Frame aParent = m_aStack.getLastOrNull ();
          final String sKey = _token (aReader.getNamespaceURI ()) + aReader.getLocalName ();
          if (!m_bCII && aParent == aRoot && ("cac:InvoiceLine".equals (sKey) || "cac:CreditNoteLine".equals (sKey)))
            break;
          final boolean bFirstOfName = aParent.m_aSeenChildren.add (sKey);
          final String sPath = aParent == aRoot ? sKey : aParent.m_sPath + '/' + sKey;
          if (!bFirstOfName && !REPEATABLE.contains (sKey))
            _skipElement (aReader);
          else
            if (m_aLeaves.contains (sPath))
              _onLeaf (aReader, sPath);
            else
              if (m_aBranches.contains (sPath))
              {
                m_aStack.add (new Frame (sPath));
                _onBranchStart (sPath);
              }
              else
                _skipElement (aReader);
        }
        else
          if (nEvent == XMLStreamConstants.END_ELEMENT)
          {
            final Frame aFrame = m_aStack.removeLastOrNull ();
            _onBranchEnd (aFrame.m_sPath);
            if (m_bCII && aFrame.m_sPath.equals (CII_AGREEMENT))
              break;
          }
      }
      return _build (m_bCII ? HybridXmlContext.SYNTAX_CII : HybridXmlContext.SYNTAX_UBL,
                     m_sCustomizationID,
                     m_sBusinessProcess,
                     m_aVatID[PARTY_SELLER],
                     m_aPostalCountry[PARTY_SELLER],
                     m_aVatID[PARTY_TAX_REP],
                     m_aVatID[PARTY_BUYER],
                     m_aPostalCountry[PARTY_BUYER]);
    }
  }
}
