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
package com.helger.kaltblut.core.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Test;

import com.helger.kaltblut.core.extract.HybridExtractor;
import com.helger.kaltblut.core.inspect.HybridInspector;
import com.helger.kaltblut.testfiles.KaltblutTestFiles;

/**
 * Test class for class {@link HybridLimits}.
 *
 * @author Philip Helger
 */
public final class HybridLimitsTest
{
  @Test
  public void testDefaultsAreNonZero ()
  {
    assertEquals (HybridLimits.DEFAULT_MAX_PDF_BYTES, HybridLimits.DEFAULTS.getMaxPdfBytes ());
    assertEquals (HybridLimits.DEFAULT_MAX_ATTACHMENT_BYTES, HybridLimits.DEFAULTS.getMaxAttachmentBytes ());
    assertEquals (HybridLimits.DEFAULT_MAX_AGGREGATE_ATTACHMENT_BYTES,
                  HybridLimits.DEFAULTS.getMaxAggregateAttachmentBytes ());
    assertEquals (HybridLimits.DEFAULT_MAX_ATTACHMENT_COUNT, HybridLimits.DEFAULTS.getMaxAttachmentCount ());
  }

  @Test
  public void testUnlimitedDisablesAll ()
  {
    assertEquals (-1L, HybridLimits.UNLIMITED.getMaxPdfBytes ());
    assertEquals (-1L, HybridLimits.UNLIMITED.getMaxAttachmentBytes ());
    assertEquals (-1L, HybridLimits.UNLIMITED.getMaxAggregateAttachmentBytes ());
    assertEquals (-1, HybridLimits.UNLIMITED.getMaxAttachmentCount ());
  }

  @Test
  public void testWithers ()
  {
    final HybridLimits aA = HybridLimits.DEFAULTS.withMaxPdfBytes (123L);
    assertEquals (123L, aA.getMaxPdfBytes ());
    assertEquals (HybridLimits.DEFAULTS.getMaxAttachmentBytes (), aA.getMaxAttachmentBytes ());

    final HybridLimits aB = aA.withMaxAttachmentCount (5);
    assertEquals (5, aB.getMaxAttachmentCount ());
    assertEquals (123L, aB.getMaxPdfBytes ());
  }

  @Test
  public void testPdfSizeLimitRejects () throws IOException
  {
    final IHybridSource aSource = HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_1_EN16931_EMBEDDED);
    // The sample PDF is well over 1 KB; reading with a 1 KB cap must throw.
    final HybridLimits aTiny = HybridLimits.DEFAULTS.withMaxPdfBytes (1024L);
    assertThrows (IOException.class, () -> HybridInspector.readMetadata (aSource, aTiny));
  }

  @Test
  public void testAttachmentCountLimitRejects () throws IOException
  {
    final IHybridSource aSource = HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_1_EN16931_EMBEDDED);
    // This sample has the invoice XML + supporting attachments. A cap of 0 must reject.
    final HybridLimits aZeroCount = HybridLimits.DEFAULTS.withMaxAttachmentCount (0);
    assertThrows (IOException.class, () -> HybridExtractor.listAttachments (aSource, aZeroCount));
  }

  @Test
  public void testDefaultsPassForSamplePdf () throws IOException
  {
    final IHybridSource aSource = HybridSource.fromClasspath (KaltblutTestFiles.ZF_2_3_MINIMUM);
    assertNotNull (HybridInspector.readMetadata (aSource));
  }
}
