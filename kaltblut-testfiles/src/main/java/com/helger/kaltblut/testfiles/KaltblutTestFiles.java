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
package com.helger.kaltblut.testfiles;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;

/**
 * Classpath-resource paths for the representative ZUGFeRD / Factur-X sample PDFs used by the
 * kaltblut test suites.
 * <p>
 * One sample per distinct PDF-carrier generation captured in
 * {@code docs/requirements/comparison.md}. Each constant is a resource path suitable for use with
 * {@link ClassLoader#getResourceAsStream(String)} or the corresponding {@code HybridSource}
 * factory.
 *
 * @author Philip Helger
 */
@Immutable
public final class KaltblutTestFiles
{
  private static final String PREFIX = "external/zugferd/";

  /**
   * ZUGFeRD 2.0.1 sample with the EN16931 profile. Legacy {@code urn:zugferd:...:2p0} extension
   * schema, embedded XML named {@code zugferd-invoice.xml}, AFRelationship {@code Alternative}.
   */
  public static final String ZF_2_0_1_EN16931 = PREFIX + "2.0.1/zugferd_2p0_EN16931_Einfach.pdf";

  /**
   * ZUGFeRD 2.1 sample with the EN16931 profile and embedded supporting documents. First appearance
   * of the {@code urn:factur-x:...:1p0} primary namespace; embedded XML named {@code factur-x.xml}.
   */
  public static final String ZF_2_1_EN16931_EMBEDDED = PREFIX + "2.1/zugferd_2p1_EN16931_Elektron_embedded.pdf";

  /**
   * ZUGFeRD 2.2 XRECHNUNG-profile sample. First appearance of the XRECHNUNG reference profile and
   * the {@code xrechnung.xml} embedded file name. AFRelationship {@code Source}.
   */
  public static final String ZF_2_2_XRECHNUNG = PREFIX + "2.2/XRECHNUNG_Einfach.pdf";

  /**
   * ZUGFeRD 2.3 MINIMUM-profile sample. AFRelationship {@code Data} (MINIMUM cannot be the sole
   * data source).
   */
  public static final String ZF_2_3_MINIMUM = PREFIX + "2.3/MINIMUM_Buchungshilfe.pdf";

  /**
   * ZUGFeRD 2.4 BASIC WL sample. PDF carrier rules are identical to 2.3.
   * <p>
   * Note: the ZUGFeRD 2.5 release shipped this exact same PDF (byte-identical) as its BASIC WL
   * sample, so this resource doubles as the 2.5 sample. PDF-carrier-wise 2.5 == 2.4, with the
   * single exception of {@code XLS} being added to the §6.4 attachment whitelist.
   */
  public static final String ZF_2_4_BASIC_WL = PREFIX + "2.4/BASIC-WL_Einfach_fx.pdf";

  private KaltblutTestFiles ()
  {}

  /**
   * Lookup the classpath resource bytes for the given path.
   *
   * @param sResourcePath
   *        the resource path. May not be <code>null</code>.
   * @return the resource bytes, or <code>null</code> if the resource is not on the classpath.
   * @throws IOException
   *         on read failure.
   */
  @Nullable
  public static byte [] readBytes (@NonNull final String sResourcePath) throws IOException
  {
    try (final InputStream aIS = KaltblutTestFiles.class.getClassLoader ().getResourceAsStream (sResourcePath))
    {
      if (aIS == null)
        return null;

      return aIS.readAllBytes ();
    }
  }
}
