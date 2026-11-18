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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.testfiles.KaltblutTestFiles;

/**
 * Test class for class {@link HybridValidator}.
 *
 * @author Philip Helger
 */
public final class HybridValidatorTest
{
  @NonNull
  private static HybridValidator _newValidator (@NonNull final EZugferdCountry eCountry)
  {
    final HybridValidator v = new HybridValidator ();
    v.getSettings ().setCountry (eCountry).setCheckPdfA3 (false);
    return v;
  }

  /**
   * Return the bytes of a minimal valid PDF (no XMP / no embedded files).
   */
  @NonNull
  private static byte [] _pdfWithoutInvoiceXml ()
  {
    final String sPdf = "%PDF-1.4\n" +
                        "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                        "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                        "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Resources<<>>>>endobj\n" +
                        "xref\n" +
                        "0 4\n" +
                        "0000000000 65535 f \n" +
                        "0000000009 00000 n \n" +
                        "0000000052 00000 n \n" +
                        "0000000101 00000 n \n" +
                        "trailer<</Size 4/Root 1 0 R>>\n" +
                        "startxref\n" +
                        "173\n" +
                        "%%EOF\n";
    return sPdf.getBytes (StandardCharsets.US_ASCII);
  }

  private static void _rewriteEmbeddedXml (@NonNull final PDDocument aDoc,
                                           @NonNull final PDNameTreeNode <PDComplexFileSpecification> aNode,
                                           @NonNull final UnaryOperator <String> aXmlModifier,
                                           @Nullable final String sAFRelationship) throws IOException
  {
    if (aNode.getKids () != null)
      for (final PDNameTreeNode <PDComplexFileSpecification> aKid : aNode.getKids ())
        _rewriteEmbeddedXml (aDoc, aKid, aXmlModifier, sAFRelationship);
    if (aNode.getNames () == null)
      return;
    for (final Map.Entry <String, PDComplexFileSpecification> aEntry : aNode.getNames ().entrySet ())
    {
      final PDComplexFileSpecification aSpec = aEntry.getValue ();
      final PDEmbeddedFile aOld = aSpec.getEmbeddedFileUnicode () != null ? aSpec.getEmbeddedFileUnicode ()
                                                                          : aSpec.getEmbeddedFile ();
      final String sXml = aXmlModifier.apply (new String (StreamHelper.getAllBytes (aOld.createInputStream ()),
                                                          StandardCharsets.UTF_8));
      final PDEmbeddedFile aNew = new PDEmbeddedFile (aDoc,
                                                      new NonBlockingByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8)));
      aNew.setSubtype (aOld.getSubtype ());
      aSpec.setEmbeddedFile (aNew);
      aSpec.setEmbeddedFileUnicode (aNew);
      if (sAFRelationship != null)
        aSpec.getCOSObject ().setName (COSName.getPDFName ("AFRelationship"), sAFRelationship);
    }
  }

  @NonNull
  private static String _modifyParties (@NonNull final String sOriginalXml,
                                        @Nullable final String sSellerVat,
                                        @Nullable final String sBuyerCountry)
  {
    String sXml = sOriginalXml;
    if (sSellerVat != null)
      sXml = sXml.replace (">DE123456789<", ">" + sSellerVat + "<");
    if (sBuyerCountry != null)
    {
      final int nBuyer = sXml.indexOf ("<ram:BuyerTradeParty>");
      final int nCountry = sXml.indexOf ("<ram:CountryID>DE</ram:CountryID>", nBuyer);
      sXml = sXml.substring (0, nCountry) +
             "<ram:CountryID>" +
             sBuyerCountry +
             "</ram:CountryID>" +
             sXml.substring (nCountry + "<ram:CountryID>DE</ram:CountryID>".length ());
    }
    return sXml;
  }

  @NonNull
  private static byte [] _sampleWith (@NonNull final String sClasspath,
                                      @NonNull final UnaryOperator <String> aXmlModifier,
                                      @Nullable final String sAFRelationship) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (HybridSource.fromClasspath (sClasspath).getBytes ());
         final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      _rewriteEmbeddedXml (aDoc,
                           aDoc.getDocumentCatalog ().getNames ().getEmbeddedFiles (),
                           aXmlModifier,
                           sAFRelationship);
      aDoc.save (aBAOS);
      return aBAOS.toByteArray ();
    }
  }

  /**
   * Copy a sample and rewrite its embedded invoice XML (seller VAT ID, buyer postal country) and
   * /AFRelationship. The samples all have seller VAT DE123456789 and a DE buyer. An empty buyer
   * country makes the buyer country unknown.
   */
  @NonNull
  private static byte [] _sampleWith (@NonNull final String sClasspath,
                                      @Nullable final String sSellerVat,
                                      @Nullable final String sBuyerCountry,
                                      @Nullable final String sAFRelationship) throws IOException
  {
    return _sampleWith (sClasspath, x -> _modifyParties (x, sSellerVat, sBuyerCountry), sAFRelationship);
  }

  @NonNull
  private static HybridValidationResult _validateAuto (@NonNull final byte [] aPdf) throws IOException
  {
    final HybridValidator v = _newValidator (EZugferdCountry.OTHER);
    v.getSettings ().setReadXmlContext (true);
    return v.validate (HybridSource.fromBytes (aPdf));
  }

  @Test
  public void testRejectsNonHybridPdf () throws IOException
  {
    // A tiny hand-crafted PDF with no XMP / no embedded files must trigger BR-HYBRID-03.
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.OTHER).validate (HybridSource.fromBytes (_pdfWithoutInvoiceXml ()));
    assertTrue ("Expected BR-HYBRID-03 fatal for non-hybrid PDF", aRes.hasErrorRule ("BR-HYBRID-03"));
  }

  @Test
  public void testValidLegacyInvoice_NoFatalFindings () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    assertFalse ("Unexpected fatal findings: " + aRes.getFindings (EHybridSeverity.ERROR), aRes.hasError ());
    assertTrue (aRes.isValid ());
  }

  @Test
  public void testMinimumProfile_DE_TriggersBrHybridDe01 () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertTrue ("BR-HYBRID-DE-01 must fire for MINIMUM in DE", aRes.hasErrorRule ("BR-HYBRID-DE-01"));
  }

  @Test
  public void testMinimumProfile_FR_DoesNotTriggerBrHybridDe01 () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.FR).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertFalse (aRes.hasRule ("BR-HYBRID-DE-01"));
  }

  @Test
  public void testBasicWlProfile_DE_TriggersBrHybridDe02 () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_4_BASIC_WL));
    assertTrue ("BR-HYBRID-DE-02 must fire for BASIC WL in DE", aRes.hasErrorRule ("BR-HYBRID-DE-02"));
  }

  @Test
  public void testXRechnungProfile_FR_TriggersBrHybridFr01 () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.FR).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    assertTrue ("BR-HYBRID-FR-01 must fire for XRECHNUNG in FR", aRes.hasErrorRule ("BR-HYBRID-FR-01"));
    // n/a in the AFRelationship matrix - only reported by BR-HYBRID-FR-01
    assertFalse (aRes.hasRule ("BR-HYBRID-11"));
  }

  @Test
  public void testXRechnungProfile_DE_DoesNotTriggerBrHybridFr01 () throws IOException
  {
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    assertFalse (aRes.hasRule ("BR-HYBRID-FR-01"));
  }

  @Test
  public void testXRechnungSample_HasVersionWarning () throws IOException
  {
    // The shipped 2.2 XRECHNUNG sample uses fx:Version="2.1" instead of "1.0", which should produce
    // BR-HYBRID-10 as a Warning (not Fatal).
    final HybridValidationResult aRes = _newValidator (EZugferdCountry.OTHER).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    final HybridFinding aF = aRes.findByRuleID ("BR-HYBRID-10");
    assertNotNull ("BR-HYBRID-10 must be reported when fx:Version != 1.0", aF);
    assertTrue (aF.getSeverity () == EHybridSeverity.WARNING);
  }

  @Test
  public void testReadXmlContext_AutoCountryDE () throws IOException
  {
    // Country is OTHER, so the seller country DE is taken from the embedded XML
    final HybridValidator v = _newValidator (EZugferdCountry.OTHER);
    v.getSettings ().setReadXmlContext (true);
    final HybridValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertTrue ("BR-HYBRID-DE-01 must fire for auto-detected DE", aRes.hasErrorRule ("BR-HYBRID-DE-01"));
    final HybridFinding aF = aRes.findByRuleID ("KALTBLUT-COUNTRY-AUTO");
    assertNotNull ("Auto-detected country must be reported", aF);
    assertEquals (EHybridSeverity.INFORMATION, aF.getSeverity ());
    // The MINIMUM sample has no buyer country
    assertTrue (aF.getMessage ().contains ("treated as DE"));
  }

  @Test
  public void testReadXmlContext_DisabledByDefault () throws IOException
  {
    // Carrier-only default path: the XML is not consulted, so the DE rules don't fire
    final HybridValidator v = _newValidator (EZugferdCountry.OTHER);
    assertFalse (v.getSettings ().isReadXmlContext ());
    final HybridValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertFalse (aRes.hasRule ("BR-HYBRID-DE-01"));
    assertFalse (aRes.hasRule ("KALTBLUT-COUNTRY-AUTO"));
  }

  @Test
  public void testReadXmlContext_ExplicitCountryWins () throws IOException
  {
    // The XML says DE, but the explicitly configured FR takes precedence
    final HybridValidator v = _newValidator (EZugferdCountry.FR);
    v.getSettings ().setReadXmlContext (true);
    final HybridValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertFalse (aRes.hasRule ("BR-HYBRID-DE-01"));
    assertFalse (aRes.hasRule ("KALTBLUT-COUNTRY-AUTO"));
  }

  @Test
  public void testReadXmlContext_NonHybridPdf () throws IOException
  {
    // No invoice XML: the country stays OTHER, which is reported, and validation continues normally
    final HybridValidator v = _newValidator (EZugferdCountry.OTHER);
    v.getSettings ().setReadXmlContext (true);
    final HybridValidationResult aRes = v.validate (HybridSource.fromBytes (_pdfWithoutInvoiceXml ()));
    assertTrue (aRes.hasErrorRule ("BR-HYBRID-03"));
    final HybridFinding aF = aRes.findByRuleID ("KALTBLUT-COUNTRY-AUTO");
    assertNotNull ("Failed auto-detection must be reported", aF);
    assertTrue (aF.getMessage ().contains ("could not be read"));
  }

  @Test
  public void testReadXmlContext_DeProfileRulesFollowBuyer () throws IOException
  {
    // DE↔DE: the specification rule BR-HYBRID-DE-02 applies
    HybridValidationResult aRes = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_4_BASIC_WL, null, "DE", null));
    assertTrue (aRes.hasErrorRule ("BR-HYBRID-DE-02"));
    assertFalse (aRes.hasRule ("KALTBLUT-DE-02"));
    // DE to any other country: the non-standard KALTBLUT-DE-02 warning instead
    for (final String sBuyer : new String [] { "FR", "AT", "NO", "CH", "GB", "US" })
    {
      aRes = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_4_BASIC_WL, null, sBuyer, null));
      assertFalse (sBuyer, aRes.hasRule ("BR-HYBRID-DE-02"));
      final HybridFinding aF = aRes.findByRuleID ("KALTBLUT-DE-02");
      assertNotNull (sBuyer, aF);
      assertEquals (sBuyer, EHybridSeverity.WARNING, aF.getSeverity ());
    }
    assertTrue (aRes.findByRuleID ("KALTBLUT-COUNTRY-AUTO").getMessage ().contains ("buyer is not in DE"));
  }

  @Test
  public void testReadXmlContext_DeAfMatrixRequiresDomestic () throws IOException
  {
    // EN 16931 with /AFRelationship Data: the DE matrix (expects Alternative) only applies DE↔DE
    assertTrue (_validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_0_1_EN16931, null, null, "Data")).hasRule (
                                                                                                              "BR-HYBRID-11"));
    assertFalse (_validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_0_1_EN16931, null, "FR", "Data")).hasRule (
                                                                                                               "BR-HYBRID-11"));
  }

  @Test
  public void testReadXmlContext_FrRulesRequireDomestic () throws IOException
  {
    // FR seller with XRECHNUNG: BR-HYBRID-FR-01 only applies FR↔FR
    assertTrue (_validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_2_XRECHNUNG, "FR12345678901", "FR", null))
                                                                                                             .hasErrorRule ("BR-HYBRID-FR-01"));
    final HybridValidationResult aRes = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_2_XRECHNUNG,
                                                                    "FR12345678901",
                                                                    null,
                                                                    null));
    assertFalse (aRes.hasRule ("BR-HYBRID-FR-01"));
    assertTrue (aRes.findByRuleID ("KALTBLUT-COUNTRY-AUTO").getMessage ().contains ("buyer is not in FR"));
    // An unknown buyer (empty country) counts as FR
    final HybridValidationResult aResUnknown = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_2_XRECHNUNG,
                                                                           "FR12345678901",
                                                                           "",
                                                                           null));
    assertTrue (aResUnknown.hasErrorRule ("BR-HYBRID-FR-01"));
    assertTrue (aResUnknown.findByRuleID ("KALTBLUT-COUNTRY-AUTO").getMessage ().contains ("treated as FR"));
  }

  @Test
  public void testReadXmlContext_SellerNotDeOrFr () throws IOException
  {
    // Readable XML, but the seller is neither in DE nor in FR
    final HybridFinding aF = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_0_1_EN16931, "ATU12345678", null, null))
                                                                                                                        .findByRuleID ("KALTBLUT-COUNTRY-AUTO");
    assertTrue (aF.getMessage ().startsWith ("No DE or FR"));
  }

  @Test
  public void testReadXmlContext_ArbitraryXml () throws IOException
  {
    // The invoice XML attachment of a real sample carries something else: the country stays OTHER,
    // the failed detection is reported and validation continues normally
    for (final String sXml : new String [] { "<?xml version='1.0'?><catalog><book id='1'>Title</book></catalog>",
                                             "<Invoice><ID>1</ID></Invoice>",
                                             "this is not XML at all",
                                             "<?xml version='1.0'?><!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><x>&e;</x>" })
    {
      final HybridValidationResult aRes = _validateAuto (_sampleWith (KaltblutTestFiles.ZF_2_3_MINIMUM,
                                                                      x -> sXml,
                                                                      null));
      assertFalse (sXml, aRes.hasRule ("BR-HYBRID-DE-01"));
      assertTrue (sXml, aRes.findByRuleID ("KALTBLUT-COUNTRY-AUTO").getMessage ().contains ("could not be read"));
    }
  }

  @Test
  public void testPdfA3SpiAbsentEmitsInformation () throws IOException
  {
    // kaltblut-core has no IPdfA3Validator on the classpath, so PDF/A-3 validation should produce
    // exactly one INFORMATION finding (rather than fatal-failing).
    final HybridValidator v = new HybridValidator ();
    v.getSettings ().setCountry (EZugferdCountry.OTHER).setCheckPdfA3 (true);
    final HybridValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    final HybridFinding aF = aRes.findByRuleID ("KALTBLUT-PDFA-SPI-MISSING");
    assertNotNull ("Without veraPDF on classpath the validator must record an SPI-missing INFO finding", aF);
    assertTrue (aF.getSeverity () == EHybridSeverity.INFORMATION);
  }
}
