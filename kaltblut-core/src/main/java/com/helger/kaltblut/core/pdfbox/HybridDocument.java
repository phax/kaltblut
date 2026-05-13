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
package com.helger.kaltblut.core.pdfbox;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.annotation.WillClose;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.kaltblut.core.model.EAFRelationship;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.model.EZugferdProfile;
import com.helger.kaltblut.core.model.HybridAttachment;
import com.helger.kaltblut.core.model.HybridMetadata;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.xml.serialize.read.DOMReader;

/**
 * Opens a hybrid-invoice PDF via PDFBox, parses XMP and embedded-files metadata, and exposes a
 * consolidated view for the inspector / extractor / validator. The caller is responsible for
 * closing the document.
 * <p>
 * This class is package-public for use by the other kaltblut-core modules; consumers should
 * normally go through {@code HybridInspector} / {@code HybridExtractor} / {@code HybridValidator}.
 *
 * @author Philip Helger
 */
public final class HybridDocument implements AutoCloseable
{
  /** Local names we care about under each known flavor's XMP extension-schema namespace. */
  private static final String XMP_DOCUMENT_TYPE = "DocumentType";
  private static final String XMP_DOCUMENT_FILE_NAME = "DocumentFileName";
  private static final String XMP_VERSION = "Version";
  private static final String XMP_CONFORMANCE_LEVEL = "ConformanceLevel";

  /** PDF/A "Associated File" keys. */
  private static final COSName COSNAME_AF = COSName.getPDFName ("AF");
  private static final COSName COSNAME_AFRELATIONSHIP = COSName.getPDFName ("AFRelationship");

  private static final Logger LOGGER = LoggerFactory.getLogger (HybridDocument.class);

  private final PDDocument m_aDoc;
  private final IHybridSource m_aSource;
  private final HybridLimits m_aLimits;
  private HybridMetadata m_aCachedMetadata;
  private ICommonsList <HybridAttachment> m_aCachedAttachments;

  private HybridDocument (@NonNull final PDDocument aDoc,
                          @NonNull final IHybridSource aSource,
                          @NonNull final HybridLimits aLimits)
  {
    m_aDoc = aDoc;
    m_aSource = aSource;
    m_aLimits = aLimits;
  }

  /**
   * Open a hybrid-invoice PDF using {@link HybridLimits#DEFAULTS}.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return an opened document; close it via {@link #close()}.
   * @throws IOException
   *         on I/O or PDF-parsing failure, or if the source exceeds the default size limit.
   */
  @NonNull
  public static HybridDocument open (@NonNull final IHybridSource aSource) throws IOException
  {
    return open (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * Open a hybrid-invoice PDF, enforcing the given byte / count ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>; use {@link HybridLimits#UNLIMITED} to disable.
   * @return an opened document; close it via {@link #close()}.
   * @throws IOException
   *         on I/O or PDF-parsing failure, or if the source exceeds
   *         <code>aLimits.getMaxPdfBytes()</code>.
   */
  @NonNull
  public static HybridDocument open (@NonNull final IHybridSource aSource, @NonNull final HybridLimits aLimits)
                                                                                                                throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    final byte [] aBytes = aSource.getBytes (aLimits.getMaxPdfBytes ());
    final PDDocument aDoc = Loader.loadPDF (aBytes);
    return new HybridDocument (aDoc, aSource, aLimits);
  }

  /**
   * @return the underlying PDFBox document.
   */
  @NonNull
  public PDDocument getPDDocument ()
  {
    return m_aDoc;
  }

  /**
   * @return the source this document was opened from.
   */
  @NonNull
  public IHybridSource getSource ()
  {
    return m_aSource;
  }

  /** @return the limits in effect for this document. Never <code>null</code>. */
  @NonNull
  public HybridLimits getLimits ()
  {
    return m_aLimits;
  }

  /**
   * Read the hybrid-invoice metadata. Result is cached for the lifetime of this document.
   *
   * @return the metadata snapshot. Never <code>null</code>.
   * @throws IOException
   *         on PDF / XMP parsing failure.
   */
  @NonNull
  public HybridMetadata readMetadata () throws IOException
  {
    if (m_aCachedMetadata == null)
      m_aCachedMetadata = _doReadMetadata ();
    return m_aCachedMetadata;
  }

  /**
   * Find the file specification that represents the invoice XML attached to the document Catalog's
   * /AF array, preferring the one whose name matches the XMP <code>fx:DocumentFileName</code>.
   * Falls back to the default name for the detected flavor, or the first AF entry overall.
   */
  @Nullable
  private static PDComplexFileSpecification _findInvoiceFileSpec (@NonNull final PDDocumentCatalog aCatalog,
                                                                  @Nullable final String sXmpDocumentFileName,
                                                                  @Nullable final EZugferdFlavor eFlavor)
  {
    final COSDictionary aCatalogDict = aCatalog.getCOSObject ();
    final COSBase aAFObj = aCatalogDict.getDictionaryObject (COSNAME_AF);
    if (!(aAFObj instanceof final COSArray aAF))
      return null;

    PDComplexFileSpecification aMatchByXmp = null;
    PDComplexFileSpecification aMatchByFlavor = null;
    PDComplexFileSpecification aFirstSpec = null;
    for (int i = 0; i < aAF.size (); i++)
    {
      final COSBase aItem = aAF.getObject (i);
      if (!(aItem instanceof final COSDictionary aDict))
        continue;

      final PDComplexFileSpecification aSpec = new PDComplexFileSpecification (aDict);
      if (aFirstSpec == null)
        aFirstSpec = aSpec;

      final String sName = aSpec.getFileUnicode () != null ? aSpec.getFileUnicode () : aSpec.getFile ();
      if (sName == null)
        continue;

      if (sXmpDocumentFileName != null && sXmpDocumentFileName.equals (sName))
      {
        aMatchByXmp = aSpec;
        break;
      }
      if (eFlavor != null && eFlavor.getDefaultEmbeddedFileName ().equals (sName))
        aMatchByFlavor = aSpec;
    }

    // Decide which one to use
    if (aMatchByXmp != null)
      return aMatchByXmp;
    if (aMatchByFlavor != null)
      return aMatchByFlavor;
    return aFirstSpec;
  }

  /**
   * Scan an XMP DOM for any element or attribute in one of the known flavor namespaces. Collects
   * the four expected local names from elements and attributes alike (both forms appear in the
   * spec: element form and attribute form).
   *
   * @param aXmpDoc
   *        XML DOM document
   * @return Scan result or <code>null</code>.
   */
  @Nullable
  private static ScanResult _scanXmpForFlavor (@NonNull final Document aXmpDoc)
  {
    final Element aRoot = aXmpDoc.getDocumentElement ();
    if (aRoot == null)
      return null;

    for (final EZugferdFlavor eCandidate : EZugferdFlavor.values ())
    {
      final ICommonsMap <String, String> aFields = new CommonsHashMap <> ();
      _collectFieldsForNamespaceRecursive (aRoot, eCandidate.getNamespaceURI (), aFields);
      if (aFields.isNotEmpty ())
        return new ScanResult (eCandidate.getNamespaceURI (), aFields);
    }
    return null;
  }

  @NonNull
  private HybridMetadata _doReadMetadata () throws IOException
  {
    final PDDocumentCatalog aCatalog = m_aDoc.getDocumentCatalog ();

    // ----- XMP -----
    String sNamespaceURI = null;
    EZugferdFlavor eFlavor = null;
    final ICommonsMap <String, String> aXmpFields = new CommonsHashMap <> ();
    final PDMetadata aMetadata = aCatalog.getMetadata ();
    if (aMetadata != null)
    {
      final byte [] aXmpBytes = StreamHelper.getAllBytes (aMetadata.createInputStream ());
      if (aXmpBytes != null && aXmpBytes.length > 0)
      {
        final Document aXmpDoc = DOMReader.readXMLDOM (aXmpBytes);
        if (aXmpDoc != null)
        {
          // Scan every element + attribute for a namespace we recognise.
          final ScanResult aRes = _scanXmpForFlavor (aXmpDoc);
          if (aRes != null)
          {
            sNamespaceURI = aRes.m_sNamespaceURI;
            eFlavor = EZugferdFlavor.getFromNamespaceURIOrNull (sNamespaceURI);
            aXmpFields.putAll (aRes.m_aFields);
          }
        }
        else
          LOGGER.warn ("Failed to parse XMP metadata as XML in PDF '" + m_aSource.getName () + "'");
      }
    }

    final String sXmpDocumentType = aXmpFields.get (XMP_DOCUMENT_TYPE);
    final String sXmpDocumentFileName = aXmpFields.get (XMP_DOCUMENT_FILE_NAME);
    final String sXmpVersion = aXmpFields.get (XMP_VERSION);
    final String sRawProfile = aXmpFields.get (XMP_CONFORMANCE_LEVEL);
    final EZugferdProfile eProfile = EZugferdProfile.getFromIDOrNull (sRawProfile);

    // ----- Document-level /AF -----
    String sEmbeddedFileName = null;
    EAFRelationship eAFRelationship = null;
    String sRawAFRelationship = null;
    final PDComplexFileSpecification aInvoiceSpec = _findInvoiceFileSpec (aCatalog, sXmpDocumentFileName, eFlavor);
    if (aInvoiceSpec != null)
    {
      sEmbeddedFileName = aInvoiceSpec.getFileUnicode () != null ? aInvoiceSpec.getFileUnicode () : aInvoiceSpec
                                                                                                                .getFile ();
      sRawAFRelationship = aInvoiceSpec.getCOSObject ().getNameAsString (COSNAME_AFRELATIONSHIP);
      eAFRelationship = EAFRelationship.getFromIDOrNull (sRawAFRelationship);
    }

    return new HybridMetadata (eFlavor,
                               sNamespaceURI,
                               sXmpDocumentType,
                               sXmpDocumentFileName,
                               sXmpVersion,
                               eProfile,
                               sRawProfile,
                               sEmbeddedFileName,
                               eAFRelationship,
                               sRawAFRelationship);
  }

  private static void _collectAllEmbeddedFilesRecursive (@NonNull final PDNameTreeNode <PDComplexFileSpecification> aNode,
                                                         @NonNull final ICommonsMap <String, PDComplexFileSpecification> aOut) throws IOException
  {
    final Map <String, PDComplexFileSpecification> aDirect = aNode.getNames ();
    if (aDirect != null)
      aOut.putAll (aDirect);
    final List <PDNameTreeNode <PDComplexFileSpecification>> aKids = aNode.getKids ();
    if (aKids != null)
      for (final PDNameTreeNode <PDComplexFileSpecification> aKid : aKids)
        _collectAllEmbeddedFilesRecursive (aKid, aOut);
  }

  @NonNull
  private static ICommonsMap <String, PDComplexFileSpecification> _collectAllEmbeddedFiles (@NonNull final PDEmbeddedFilesNameTreeNode aNode) throws IOException
  {
    final ICommonsMap <String, PDComplexFileSpecification> aResult = new CommonsHashMap <> ();
    _collectAllEmbeddedFilesRecursive (aNode, aResult);
    return aResult;
  }

  @Nullable
  private static PDEmbeddedFile _pickEmbeddedFileStream (@NonNull final PDComplexFileSpecification aSpec)
  {
    PDEmbeddedFile aEF = aSpec.getEmbeddedFileUnicode ();
    if (aEF == null)
      aEF = aSpec.getEmbeddedFile ();
    if (aEF == null)
      aEF = aSpec.getEmbeddedFileDos ();
    if (aEF == null)
      aEF = aSpec.getEmbeddedFileMac ();
    if (aEF == null)
      aEF = aSpec.getEmbeddedFileUnix ();
    return aEF;
  }

  /**
   * Read at most <code>nMaxBytes</code> bytes from the given input stream into a byte array. A
   * value of {@code -1} for <code>nMaxBytes</code> disables the limit (equivalent to
   * {@link StreamHelper#getAllBytes(InputStream)}). The stream is consumed but not closed.
   */
  @NonNull
  private static byte [] _readBounded (@NonNull final InputStream aIS,
                                       final long nMaxBytes,
                                       @NonNull final String sWhat) throws IOException
  {
    if (nMaxBytes < 0)
    {
      final byte [] aAll = StreamHelper.getAllBytes (aIS);
      return aAll != null ? aAll : new byte [0];
    }
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      final byte [] aBuf = new byte [8192];
      long nTotal = 0L;
      int nRead;
      while ((nRead = aIS.read (aBuf)) > 0)
      {
        nTotal += nRead;
        if (nTotal > nMaxBytes)
          throw new IOException (sWhat + " exceeded limit of " + nMaxBytes + " bytes");
        aBAOS.write (aBuf, 0, nRead);
      }
      return aBAOS.toByteArray ();
    }
  }

  @NonNull
  private ICommonsList <HybridAttachment> _doListAttachments () throws IOException
  {
    final ICommonsList <HybridAttachment> ret = new CommonsArrayList <> ();
    final HybridMetadata aMeta = readMetadata ();
    final String sInvoiceName = aMeta.getEmbeddedFileName ();

    final PDDocumentCatalog aCatalog = m_aDoc.getDocumentCatalog ();
    final PDDocumentNameDictionary aNames = aCatalog.getNames ();
    if (aNames != null)
    {
      final PDEmbeddedFilesNameTreeNode aEFTree = aNames.getEmbeddedFiles ();
      if (aEFTree != null)
      {
        final ICommonsMap <String, PDComplexFileSpecification> aAll = _collectAllEmbeddedFiles (aEFTree);

        final int nMaxCount = m_aLimits.getMaxAttachmentCount ();
        if (nMaxCount >= 0 && aAll.size () > nMaxCount)
          throw new IOException ("Embedded file count " + aAll.size () + " exceeds limit of " + nMaxCount);

        final long nMaxPer = m_aLimits.getMaxAttachmentBytes ();
        final long nMaxAggregate = m_aLimits.getMaxAggregateAttachmentBytes ();
        long nAggregate = 0L;

        for (final var aEntry : aAll.entrySet ())
        {
          final String sName = aEntry.getKey ();
          final PDComplexFileSpecification aSpec = aEntry.getValue ();
          final PDEmbeddedFile aEF = _pickEmbeddedFileStream (aSpec);
          if (aEF == null)
          {
            LOGGER.warn ("Embedded file '" + sName + "' has no stream");
            continue;
          }

          final byte [] aBytes;
          try (final InputStream aIS = aEF.createInputStream ())
          {
            aBytes = _readBounded (aIS, nMaxPer, "Embedded file '" + sName + "'");
          }

          if (nMaxAggregate >= 0)
          {
            nAggregate += aBytes.length;
            if (nAggregate > nMaxAggregate)
              throw new IOException ("Aggregate embedded-file size exceeded limit of " +
                                     nMaxAggregate +
                                     " bytes at attachment '" +
                                     sName +
                                     "'");
          }

          final String sMime = aEF.getSubtype ();
          final String sRawRel = aSpec.getCOSObject ().getNameAsString (COSNAME_AFRELATIONSHIP);
          final EAFRelationship eRel = EAFRelationship.getFromIDOrNull (sRawRel);
          final Calendar aModCal = aEF.getModDate ();
          final OffsetDateTime aModDate = aModCal == null ? null : OffsetDateTime.ofInstant (aModCal.toInstant (),
                                                                                             ZoneOffset.UTC);
          final boolean bIsInvoice = sInvoiceName != null && sInvoiceName.equals (sName);
          ret.add (new HybridAttachment (sName, sMime, eRel, sRawRel, aModDate, aBytes, bIsInvoice));
        }
      }
    }
    return ret;
  }

  /**
   * List every embedded file in the PDF (invoice XML + all supporting attachments).
   *
   * @return the attachments. Empty list if the PDF has no embedded files.
   * @throws IOException
   *         on parsing failure.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridAttachment> listAttachments () throws IOException
  {
    if (m_aCachedAttachments == null)
      m_aCachedAttachments = _doListAttachments ();
    return m_aCachedAttachments.getClone ();
  }

  // ---------------- XMP DOM scanning ----------------

  /** Result of scanning an XMP DOM for a recognised flavor namespace. */
  private static final class ScanResult
  {
    final String m_sNamespaceURI;
    final ICommonsMap <String, String> m_aFields;

    ScanResult (final String sNamespaceURI, final ICommonsMap <String, String> aFields)
    {
      m_sNamespaceURI = sNamespaceURI;
      m_aFields = aFields;
    }
  }

  private static boolean _isInterestingLocalName (@NonNull final String sLocalName)
  {
    return XMP_DOCUMENT_TYPE.equals (sLocalName) ||
           XMP_DOCUMENT_FILE_NAME.equals (sLocalName) ||
           XMP_VERSION.equals (sLocalName) ||
           XMP_CONFORMANCE_LEVEL.equals (sLocalName);
  }

  private static void _collectFieldsForNamespaceRecursive (@NonNull final Node aNode,
                                                           @NonNull final String sNsURI,
                                                           @NonNull final Map <String, String> aOut)
  {
    if (aNode.getNodeType () == Node.ELEMENT_NODE)
    {
      final Element aEl = (Element) aNode;

      // Attribute-form: <rdf:Description fx:DocumentType="INVOICE" .../>
      final NamedNodeMap aAttrs = aEl.getAttributes ();
      if (aAttrs != null)
        for (int i = 0; i < aAttrs.getLength (); i++)
        {
          final Attr aAttr = (Attr) aAttrs.item (i);
          if (sNsURI.equals (aAttr.getNamespaceURI ()))
          {
            final String sLocal = aAttr.getLocalName ();
            if (sLocal != null && _isInterestingLocalName (sLocal))
              aOut.put (sLocal, aAttr.getValue ());
          }
        }

      // Element-form: <fx:DocumentType>INVOICE</fx:DocumentType>
      if (sNsURI.equals (aEl.getNamespaceURI ()))
      {
        final String sLocal = aEl.getLocalName ();
        if (sLocal != null && _isInterestingLocalName (sLocal))
        {
          final String sValue = aEl.getTextContent ();
          if (sValue != null)
            aOut.put (sLocal, sValue.trim ());
        }
      }
    }

    final NodeList aKids = aNode.getChildNodes ();
    for (int i = 0; i < aKids.getLength (); i++)
      _collectFieldsForNamespaceRecursive (aKids.item (i), sNsURI, aOut);
  }

  @Override
  @WillClose
  public void close ()
  {
    try
    {
      m_aDoc.close ();
    }
    catch (final IOException ex)
    {
      LOGGER.warn ("Failed to close PDDocument cleanly", ex);
    }
  }

  /**
   * Open with {@link HybridLimits#DEFAULTS}, run a function, close. Convenience for one-shot
   * operations.
   *
   * @param aSource
   *        Source
   * @param aFn
   *        Function to run
   * @return The function response
   * @param <T>
   *        Response document type
   * @throws IOException
   *         if something goes wrong
   */
  public static <T> T withOpenDocument (@NonNull final IHybridSource aSource,
                                        @NonNull final IHybridDocumentFunction <T> aFn) throws IOException
  {
    return withOpenDocument (aSource, HybridLimits.DEFAULTS, aFn);
  }

  /**
   * Open with the given limits, run a function, close.
   *
   * @param aSource
   *        Source
   * @param aLimits
   *        Limits
   * @param aFn
   *        Function to run
   * @return The function response
   * @param <T>
   *        Response document type
   * @throws IOException
   *         if something goes wrong
   */
  public static <T> T withOpenDocument (@NonNull final IHybridSource aSource,
                                        @NonNull final HybridLimits aLimits,
                                        @NonNull final IHybridDocumentFunction <T> aFn) throws IOException
  {
    try (final HybridDocument aDoc = open (aSource, aLimits))
    {
      return aFn.apply (aDoc);
    }
  }
}
