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
package com.helger.kaltblut.core.validate;

import java.io.IOException;
import java.time.Duration;
import java.util.ServiceLoader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.kaltblut.core.model.EAFRelationship;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.model.EZugferdProfile;
import com.helger.kaltblut.core.model.HybridAttachment;
import com.helger.kaltblut.core.model.HybridMetadata;
import com.helger.kaltblut.core.pdfbox.HybridDocument;
import com.helger.kaltblut.core.source.IHybridSource;

/**
 * Tier 3 validation: applies the BR-HYBRID-* business rules and (when configured) PDF/A-3
 * validation via the {@link IPdfA3ValidatorSPI} SPI.
 * <p>
 * The rule set follows the Factur-X 1.07.2 / ZUGFeRD 2.3.2 specification, which is the version that
 * first published the rules with identifiers. Earlier spec versions express the same requirements
 * in prose; this validator applies the rules uniformly.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class HybridValidator
{
  // ---- HybridDocumentFilename code list ----
  private static final String FN_FACTUR_X = "factur-x.xml";
  private static final String FN_XRECHNUNG = "xrechnung.xml";
  private static final String FN_ZUGFERD_LOWER = "zugferd-invoice.xml";
  private static final String FN_ZUGFERD_MIXED = "ZUGFeRD-invoice.xml";

  private static final Logger LOGGER = LoggerFactory.getLogger (HybridValidator.class);

  private final HybridValidatorSettings m_aSettings = new HybridValidatorSettings ();

  /** @return the mutable settings object backing this validator. */
  @NonNull
  public HybridValidatorSettings getSettings ()
  {
    return m_aSettings;
  }

  /**
   * Validate the given source.
   *
   * @param aSource
   *        the source.
   * @return the aggregated result.
   * @throws IOException
   *         on I/O failure.
   */
  @NonNull
  public HybridValidationResult validate (@NonNull final IHybridSource aSource) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");

    final ICommonsList <HybridValidationLayer> aLayers = new CommonsArrayList <> ();

    // Layer 1: BR-HYBRID business rules
    final ICommonsList <HybridFinding> aBrHybridFindings = new CommonsArrayList <> ();
    boolean bXmlExtractable = false;
    final StopWatch aSwBrHybrid = StopWatch.createdStarted ();
    try (final HybridDocument aDoc = HybridDocument.open (aSource, m_aSettings.getLimits ()))
    {
      final HybridMetadata aMeta = aDoc.readMetadata ();
      final ICommonsList <HybridAttachment> aAttachments = aDoc.listAttachments ();
      for (final HybridAttachment aAtt : aAttachments)
        if (aAtt.isInvoiceXml () && aAtt.getSize () > 0)
        {
          bXmlExtractable = true;
          break;
        }
      _applyBrHybrid (aBrHybridFindings, aMeta);
    }
    aSwBrHybrid.stop ();
    aLayers.add (new HybridValidationLayer (EHybridValidationLayerKind.BR_HYBRID,
                                            Duration.ofMillis (aSwBrHybrid.getMillis ()),
                                            aBrHybridFindings));

    // Layer 2: PDF/A-3 (optional)
    if (m_aSettings.isCheckPdfA3 ())
    {
      final StopWatch aSwPdfA = StopWatch.createdStarted ();
      ICommonsList <HybridFinding> aPdfAFindings = _runPdfA3 (aSource);
      // BR-FX-DE-03: in DE↔DE, PDF/A errors are downgraded if the XML is valid and extractable.
      if (m_aSettings.isApplyDePdfADowngrade () &&
          m_aSettings.getCountry () == EZugferdCountry.DE &&
          bXmlExtractable &&
          aPdfAFindings != null &&
          !aPdfAFindings.isEmpty ())
      {
        final ICommonsList <HybridFinding> aDowngraded = new CommonsArrayList <> ();
        for (final HybridFinding aF : aPdfAFindings)
          aDowngraded.add (aF.getSeverity () == EHybridSeverity.ERROR ? aF.withSeverity (EHybridSeverity.WARNING) : aF);
        aPdfAFindings = aDowngraded;
      }
      aSwPdfA.stop ();
      aLayers.add (new HybridValidationLayer (EHybridValidationLayerKind.PDF_A3,
                                              Duration.ofMillis (aSwPdfA.getMillis ()),
                                              aPdfAFindings));
    }

    return new HybridValidationResult (aLayers);
  }

  // ===================== BR-HYBRID rules =====================

  private void _applyBrHybrid (@NonNull final ICommonsList <HybridFinding> aOut, @NonNull final HybridMetadata aMeta)
  {
    final EZugferdFlavor eFlavor = aMeta.getFlavor ();

    // BR-HYBRID-01 (Information) – informational
    aOut.add (new HybridFinding ("BR-HYBRID-01",
                                 EHybridSeverity.INFORMATION,
                                 "Hybrid document = machine-readable XML + human-readable PDF envelope.",
                                 null));

    // BR-HYBRID-03 (Fatal): a PDF/A extension schema following the prescribed structure must be
    // present.
    if (eFlavor == null)
    {
      aOut.add (new HybridFinding ("BR-HYBRID-03",
                                   EHybridSeverity.ERROR,
                                   "No recognised ZUGFeRD / Factur-X XMP extension schema was found. " +
                                                          "The PDF does not appear to be a hybrid invoice.",
                                   null));
      // Without a flavor we cannot meaningfully check the rest of the BR-HYBRID set.
      return;
    }

    // BR-HYBRID-04 (Fatal): URI must be the prescribed namespace.
    // Accept the four URIs that historically appear in real-world files.
    final String sURI = aMeta.getNamespaceURI ();
    if (eFlavor == EZugferdFlavor.FACTURX_BR_HYBRID_04)
      aOut.add (new HybridFinding ("BR-HYBRID-04",
                                   EHybridSeverity.WARNING,
                                   "Namespace URI '" +
                                                            sURI +
                                                            "' matches the BR-HYBRID-04 wording but disagrees with §6.3.1 (missing ':invoice' segment). " +
                                                            "Treat as a known irregularity.",
                                   "/Metadata"));

    // BR-HYBRID-05 (Fatal): namespace prefix MUST be fx (except for the ZUGFeRD legacy schemas
    // where 'zf' is correct).
    // The prefix is not currently parsed out (we identify by URI). Skipped as a separate rule –
    // BR-HYBRID-03 covers
    // recognition.

    // BR-HYBRID-06 (Fatal): DocumentType from the HybridDocumentType code list. Only 'INVOICE' is
    // defined.
    final String sDocType = aMeta.getXmpDocumentType ();
    if (sDocType == null)
      aOut.add (new HybridFinding ("BR-HYBRID-06",
                                   EHybridSeverity.ERROR,
                                   "XMP DocumentType is missing. Expected 'INVOICE'.",
                                   "/Metadata"));
    else
      if (!"INVOICE".equals (sDocType))
        aOut.add (new HybridFinding ("BR-HYBRID-06",
                                     EHybridSeverity.ERROR,
                                     "XMP DocumentType '" +
                                                            sDocType +
                                                            "' is not from the HybridDocumentType code list (expected 'INVOICE').",
                                     "/Metadata"));

    // BR-HYBRID-07 (Fatal): ConformanceLevel from the HybridConformanceType code list.
    final EZugferdProfile eProfile = aMeta.getProfile ();
    final String sRawProfile = aMeta.getRawProfile ();
    if (sRawProfile == null)
      aOut.add (new HybridFinding ("BR-HYBRID-07",
                                   EHybridSeverity.ERROR,
                                   "XMP ConformanceLevel is missing.",
                                   "/Metadata"));
    else
      if (eProfile == null)
        aOut.add (new HybridFinding ("BR-HYBRID-07",
                                     EHybridSeverity.ERROR,
                                     "XMP ConformanceLevel '" +
                                                            sRawProfile +
                                                            "' is not from the HybridConformanceType code list.",
                                     "/Metadata"));

    // BR-HYBRID-08 (Fatal): DocumentFileName from HybridDocumentFilename code list.
    final String sXmpFileName = aMeta.getXmpDocumentFileName ();
    if (sXmpFileName == null)
      aOut.add (new HybridFinding ("BR-HYBRID-08",
                                   EHybridSeverity.ERROR,
                                   "XMP DocumentFileName is missing.",
                                   "/Metadata"));
    else
      if (!_isInFilenameCodelist (sXmpFileName))
        aOut.add (new HybridFinding ("BR-HYBRID-08",
                                     EHybridSeverity.ERROR,
                                     "XMP DocumentFileName '" +
                                                            sXmpFileName +
                                                            "' is not from the HybridDocumentFilename code list.",
                                     "/Metadata"));

    // BR-HYBRID-09 (Fatal): Version from HybridDocumentVersion codelist.
    // BR-HYBRID-10 (Warning): Version SHOULD be 1.0.
    final String sVersion = aMeta.getXmpVersion ();
    if (sVersion == null)
      aOut.add (new HybridFinding ("BR-HYBRID-09", EHybridSeverity.ERROR, "XMP Version is missing.", "/Metadata"));
    else
      if (!"1.0".equals (sVersion))
        aOut.add (new HybridFinding ("BR-HYBRID-10",
                                     EHybridSeverity.WARNING,
                                     "XMP Version '" + sVersion + "' is not '1.0'.",
                                     "/Metadata"));

    // BR-HYBRID-11 (Warning): /AFRelationship SHOULD follow the profile × country matrix.
    final EAFRelationship eRel = aMeta.getAFRelationship ();
    if (eRel == null && aMeta.getEmbeddedFileName () != null)
      aOut.add (new HybridFinding ("BR-HYBRID-11",
                                   EHybridSeverity.WARNING,
                                   "/AFRelationship is missing on the embedded invoice file specification.",
                                   "/Catalog/AF"));
    else
      if (eProfile != null && eRel != null)
      {
        final HybridFinding aRel = _checkAfRelationshipMatrix (eProfile, eRel, m_aSettings.getCountry ());
        if (aRel != null)
          aOut.add (aRel);
      }

    // BR-HYBRID-12 (Fatal): embedding method must allow extraction. We've already opened the doc
    // and
    // know whether the AF entry is reachable.
    if (aMeta.getEmbeddedFileName () == null)
      aOut.add (new HybridFinding ("BR-HYBRID-12",
                                   EHybridSeverity.ERROR,
                                   "No associated invoice file was found on /Catalog/AF. The XML cannot be reliably extracted.",
                                   "/Catalog/AF"));

    // BR-HYBRID-13 (Fatal): embedded file name from HybridDocumentFilename code list.
    final String sEmbName = aMeta.getEmbeddedFileName ();
    if (sEmbName != null && !_isInFilenameCodelist (sEmbName))
      aOut.add (new HybridFinding ("BR-HYBRID-13",
                                   EHybridSeverity.ERROR,
                                   "Embedded file name '" +
                                                          sEmbName +
                                                          "' is not from the HybridDocumentFilename code list.",
                                   "/Catalog/AF"));

    // BR-HYBRID-14 (Warning): embedded file name SHOULD match fx:DocumentFileName.
    if (sEmbName != null && sXmpFileName != null && !sEmbName.equals (sXmpFileName))
      aOut.add (new HybridFinding ("BR-HYBRID-14",
                                   EHybridSeverity.WARNING,
                                   "Embedded file name '" +
                                                            sEmbName +
                                                            "' does not match XMP DocumentFileName '" +
                                                            sXmpFileName +
                                                            "'.",
                                   "/Catalog/AF"));

    // BR-HYBRID-15 (Warning): fx:ConformanceLevel SHOULD match the embedded XML profile.
    // Without parsing the XML's BT-24 we cannot cross-check; surface as INFORMATION when raw is
    // present.
    if (eProfile != null)
      aOut.add (new HybridFinding ("BR-HYBRID-15",
                                   EHybridSeverity.INFORMATION,
                                   "fx:ConformanceLevel reports '" +
                                                                eProfile.getID () +
                                                                "'. XML profile cross-check is not performed.",
                                   "/Metadata"));

    // BR-HYBRID-DE-01 / DE-02 (Fatal): MINIMUM and BASIC WL must not be used DE↔DE.
    if (m_aSettings.getCountry () == EZugferdCountry.DE)
    {
      if (eProfile == EZugferdProfile.MINIMUM)
        aOut.add (new HybridFinding ("BR-HYBRID-DE-01",
                                     EHybridSeverity.ERROR,
                                     "MINIMUM profile is not permitted for DE↔DE invoices.",
                                     null));
      if (eProfile == EZugferdProfile.BASIC_WL)
        aOut.add (new HybridFinding ("BR-HYBRID-DE-02",
                                     EHybridSeverity.ERROR,
                                     "BASIC WL profile is not permitted for DE↔DE invoices.",
                                     null));
    }
    // BR-HYBRID-FR-01 (Fatal): XRECHNUNG must not be used FR↔FR.
    if (m_aSettings.getCountry () == EZugferdCountry.FR && eProfile == EZugferdProfile.XRECHNUNG)
      aOut.add (new HybridFinding ("BR-HYBRID-FR-01",
                                   EHybridSeverity.ERROR,
                                   "XRECHNUNG reference profile is not permitted for FR↔FR invoices.",
                                   null));
  }

  // ===================== helpers =====================

  /**
   * Check the profile × country AFRelationship matrix from {@code docs/requirements/comparison.md}
   * §5.
   *
   * @return a {@link HybridFinding} if the matrix is violated, or {@code null} if the relationship
   *         is acceptable.
   */
  @Nullable
  private static HybridFinding _checkAfRelationshipMatrix (@NonNull final EZugferdProfile eProfile,
                                                           @NonNull final EAFRelationship eRel,
                                                           @NonNull final EZugferdCountry eCountry)
  {
    if (eProfile == EZugferdProfile.MINIMUM || eProfile == EZugferdProfile.BASIC_WL)
    {
      // Both FR and DE require Data
      if (eRel != EAFRelationship.DATA)
        return new HybridFinding ("BR-HYBRID-11",
                                  EHybridSeverity.WARNING,
                                  "Profile " +
                                                           eProfile.getID () +
                                                           " expects /AFRelationship 'Data' (got '" +
                                                           eRel.getID () +
                                                           "').",
                                  "/Catalog/AF");
      return null;
    }
    // For BASIC, EN 16931, EXTENDED, COMFORT, XRECHNUNG
    if (eCountry == EZugferdCountry.DE)
    {
      if (eRel != EAFRelationship.ALTERNATIVE)
        return new HybridFinding ("BR-HYBRID-11",
                                  EHybridSeverity.WARNING,
                                  "Profile " +
                                                           eProfile.getID () +
                                                           " in DE↔DE expects /AFRelationship 'Alternative' (got '" +
                                                           eRel.getID () +
                                                           "').",
                                  "/Catalog/AF");
      return null;
    }
    if (eCountry == EZugferdCountry.FR)
    {
      if (eProfile == EZugferdProfile.XRECHNUNG)
        return new HybridFinding ("BR-HYBRID-11",
                                  EHybridSeverity.WARNING,
                                  "XRECHNUNG profile is not used in France.",
                                  "/Catalog/AF");

      if (eRel != EAFRelationship.ALTERNATIVE && eRel != EAFRelationship.SOURCE && eRel != EAFRelationship.DATA)
        return new HybridFinding ("BR-HYBRID-11",
                                  EHybridSeverity.WARNING,
                                  "Profile " +
                                                           eProfile.getID () +
                                                           " in FR↔FR expects /AFRelationship 'Alternative', 'Source' or 'Data' (got '" +
                                                           eRel.getID () +
                                                           "').",
                                  "/Catalog/AF");
      return null;
    }
    // Country OTHER: any of A/S/D is acceptable for these profiles.
    if (eRel != EAFRelationship.ALTERNATIVE && eRel != EAFRelationship.SOURCE && eRel != EAFRelationship.DATA)
      return new HybridFinding ("BR-HYBRID-11",
                                EHybridSeverity.WARNING,
                                "Profile " +
                                                         eProfile.getID () +
                                                         " expects /AFRelationship 'Alternative', 'Source' or 'Data' (got '" +
                                                         eRel.getID () +
                                                         "').",
                                "/Catalog/AF");
    return null;
  }

  private static boolean _isInFilenameCodelist (@NonNull final String sName)
  {
    // Code list values per the spec versions covered. We accept ZUGFeRD 1.0's mixed case too.
    return FN_FACTUR_X.equals (sName) ||
           FN_XRECHNUNG.equals (sName) ||
           FN_ZUGFERD_LOWER.equals (sName) ||
           FN_ZUGFERD_MIXED.equals (sName);
  }

  // ---------------- PDF/A-3 SPI ----------------

  @NonNull
  private static ICommonsList <HybridFinding> _runPdfA3 (@NonNull final IHybridSource aSource) throws IOException
  {
    final ServiceLoader <IPdfA3ValidatorSPI> aLoader = ServiceLoader.load (IPdfA3ValidatorSPI.class);
    IPdfA3ValidatorSPI aValidator = null;
    for (final IPdfA3ValidatorSPI aCandidate : aLoader)
    {
      aValidator = aCandidate;
      break;
    }
    if (aValidator == null)
    {
      final ICommonsList <HybridFinding> aOut = new CommonsArrayList <> ();
      aOut.add (new HybridFinding ("KALTBLUT-PDFA-SPI-MISSING",
                                   EHybridSeverity.INFORMATION,
                                   "No IPdfA3Validator implementation is registered. PDF/A-3 conformance was not checked. " +
                                                                "Add kaltblut-verapdf to the classpath to enable veraPDF-based validation.",
                                   null));
      return aOut;
    }
    LOGGER.debug ("Running PDF/A-3 validator: " + aValidator.getClass ().getName ());
    return aValidator.validatePdfA3 (aSource);
  }
}
