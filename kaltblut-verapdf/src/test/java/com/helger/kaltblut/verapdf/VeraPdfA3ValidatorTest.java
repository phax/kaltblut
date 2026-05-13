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
package com.helger.kaltblut.verapdf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ServiceLoader;

import org.junit.Test;

import com.helger.collection.commons.ICommonsList;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.core.validate.EHybridSeverity;
import com.helger.kaltblut.core.validate.HybridFinding;
import com.helger.kaltblut.core.validate.HybridValidator;
import com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI;
import com.helger.kaltblut.core.validate.HybridValidationResult;
import com.helger.kaltblut.testfiles.KaltblutTestFiles;

/**
 * Smoke tests for the veraPDF adapter. We do not check exact rule IDs because they evolve with the
 * veraPDF version; instead we verify the wiring works end-to-end and produces structured findings
 * (rather than throwing) on a real ZUGFeRD sample.
 *
 * @author Philip Helger
 */
public final class VeraPdfA3ValidatorTest
{
  @Test
  public void testServiceLoaderRegistration ()
  {
    boolean bFound = false;
    for (final IPdfA3ValidatorSPI aV : ServiceLoader.load (IPdfA3ValidatorSPI.class))
      if (aV instanceof VeraPdfA3ValidatorSPI)
      {
        bFound = true;
        break;
      }
    assertTrue ("VeraPdfA3Validator must be discoverable via ServiceLoader", bFound);
  }

  @Test
  public void testValidate_RealSampleProducesFindings () throws IOException
  {
    final ICommonsList <HybridFinding> aFindings = new VeraPdfA3ValidatorSPI ().validatePdfA3 (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    assertNotNull (aFindings);
    // Validation must complete cleanly (no internal-error finding). Real-world ZUGFeRD samples
    // sometimes fail strict PDF/A-3 checks; we do not assert the absence of FATAL rule findings,
    // only that the validator itself did not crash.
    for (final HybridFinding aF : aFindings)
      assertTrue ("Unexpected internal validator error: " + aF, !"VERAPDF-ERROR".equals (aF.getRuleID ()));
  }

  @Test
  public void testEndToEndViaHybridValidator () throws IOException
  {
    final HybridValidator v = new HybridValidator ();
    v.getSettings ().setCountry (EZugferdCountry.OTHER).setCheckPdfA3 (true);
    final HybridValidationResult aRes = v.validate (HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_0_1_EN16931));
    // With kaltblut-verapdf on the classpath, no SPI-missing INFO finding should be present.
    assertTrue (!aRes.hasRule ("KALTBLUT-PDFA-SPI-MISSING"));
    // Validator must complete and produce at least the BR-HYBRID-01 informational finding.
    assertNotNull (aRes.findByRuleID ("BR-HYBRID-01"));
    assertNotNull (aRes.getFindings (EHybridSeverity.ERROR));
  }
}
