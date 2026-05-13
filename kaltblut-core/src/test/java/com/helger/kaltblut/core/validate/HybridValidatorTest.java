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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

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

  @Test
  public void testRejectsNonHybridPdf () throws IOException
  {
    // A tiny hand-crafted PDF with no XMP / no embedded files must trigger BR-HYBRID-03.
    final ValidationResult aRes = _newValidator (EZugferdCountry.OTHER).validate (HybridSource.fromBytes (_pdfWithoutInvoiceXml ()));
    assertTrue ("Expected BR-HYBRID-03 fatal for non-hybrid PDF", aRes.hasFatalRule ("BR-HYBRID-03"));
  }

  @Test
  public void testValidLegacyInvoice_NoFatalFindings () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    assertFalse ("Unexpected fatal findings: " + aRes.getFindings (EHybridSeverity.FATAL), aRes.hasFatal ());
    assertTrue (aRes.isValid ());
  }

  @Test
  public void testMinimumProfile_DE_TriggersBrHybridDe01 () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertTrue ("BR-HYBRID-DE-01 must fire for MINIMUM in DE", aRes.hasFatalRule ("BR-HYBRID-DE-01"));
  }

  @Test
  public void testMinimumProfile_FR_DoesNotTriggerBrHybridDe01 () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.FR).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM));
    assertFalse (aRes.hasRule ("BR-HYBRID-DE-01"));
  }

  @Test
  public void testBasicWlProfile_DE_TriggersBrHybridDe02 () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_4_BASIC_WL));
    assertTrue ("BR-HYBRID-DE-02 must fire for BASIC WL in DE", aRes.hasFatalRule ("BR-HYBRID-DE-02"));
  }

  @Test
  public void testXRechnungProfile_FR_TriggersBrHybridFr01 () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.FR).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    assertTrue ("BR-HYBRID-FR-01 must fire for XRECHNUNG in FR", aRes.hasFatalRule ("BR-HYBRID-FR-01"));
  }

  @Test
  public void testXRechnungProfile_DE_DoesNotTriggerBrHybridFr01 () throws IOException
  {
    final ValidationResult aRes = _newValidator (EZugferdCountry.DE).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    assertFalse (aRes.hasRule ("BR-HYBRID-FR-01"));
  }

  @Test
  public void testXRechnungSample_HasVersionWarning () throws IOException
  {
    // The shipped 2.2 XRECHNUNG sample uses fx:Version="2.1" instead of "1.0", which should produce
    // BR-HYBRID-10 as a Warning (not Fatal).
    final ValidationResult aRes = _newValidator (EZugferdCountry.OTHER).validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_2_XRECHNUNG));
    final HybridFinding aF = aRes.findByRuleID ("BR-HYBRID-10");
    assertNotNull ("BR-HYBRID-10 must be reported when fx:Version != 1.0", aF);
    assertTrue (aF.getSeverity () == EHybridSeverity.WARNING);
  }

  @Test
  public void testPdfA3SpiAbsentEmitsInformation () throws IOException
  {
    // kaltblut-core has no IPdfA3Validator on the classpath, so PDF/A-3 validation should produce
    // exactly one INFORMATION finding (rather than fatal-failing).
    final HybridValidator v = new HybridValidator ();
    v.getSettings ().setCountry (EZugferdCountry.OTHER).setCheckPdfA3 (true);
    final ValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    final HybridFinding aF = aRes.findByRuleID ("KALTBLUT-PDFA-SPI-MISSING");
    assertNotNull ("Without veraPDF on classpath the validator must record an SPI-missing INFO finding", aF);
    assertTrue (aF.getSeverity () == EHybridSeverity.INFORMATION);
  }
}
