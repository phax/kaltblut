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
package com.helger.flugesel.testutil;

import java.io.File;

import org.junit.Assume;

/**
 * Locator for the representative sample hybrid invoice PDFs used by the tests. Each constant points
 * at one sample from a distinct PDF-carrier generation. {@link #assumeAvailable(File)} skips the
 * calling test (rather than failing) if the sample file is not present, so the tests still pass on
 * checkouts where {@code docs/} has not been populated.
 * <p>
 * Coverage rationale (one sample per "actually different" carrier flavor / profile axis):
 * <ul>
 * <li>{@link #ZF_2_0_1_EN16931} – legacy {@code urn:zugferd:...:2p0} URI, file
 * {@code zugferd-invoice.xml}, AFRelationship {@code Alternative}.</li>
 * <li>{@link #ZF_2_1_EN16931_EMBEDDED} – Factur-X primary URI (first appearance), file
 * {@code factur-x.xml}.</li>
 * <li>{@link #ZF_2_2_XRECHNUNG} – {@code XRECHNUNG} profile + {@code xrechnung.xml} (first
 * appearance), AFRelationship {@code Source}, anomalous {@code fx:Version=2.1}.</li>
 * <li>{@link #ZF_2_3_MINIMUM} – {@code MINIMUM} profile with AFRelationship {@code Data}.</li>
 * <li>{@link #ZF_2_4_BASIC_WL} – latest spec, {@code BASIC WL} profile.</li>
 * </ul>
 *
 * @author Philip Helger
 */
public final class SamplePDFs
{
  private static final File DOCS = _findDocsDirectory ();

  /** ZUGFeRD 2.0.1 EN16931 — legacy {@code urn:zugferd} namespace, {@code Alternative}. */
  public static final File ZF_2_0_1_EN16931 = new File (DOCS,
                                                        "2.0.1/ZUGFeRD201/Beispiele/EN16931/zugferd_2p0_EN16931_Einfach.pdf");

  /** ZUGFeRD 2.1 EN16931 — Factur-X primary namespace introduced. */
  public static final File ZF_2_1_EN16931_EMBEDDED = new File (DOCS,
                                                               "2.1/EN/Samples/EN16931/zugferd_2p1_EN16931_Elektron_embedded.pdf");

  /** ZUGFeRD 2.2 XRECHNUNG — first profile that uses {@code xrechnung.xml}. */
  public static final File ZF_2_2_XRECHNUNG = new File (DOCS, "2.2/DE/Examples/XRECHNUNG/XRECHNUNG_Einfach.pdf");

  /** ZUGFeRD 2.3 MINIMUM — AFRelationship {@code Data}. */
  public static final File ZF_2_3_MINIMUM = new File (DOCS,
                                                      "2.3/Examples/0. MINIMUM/MINIMUM_Buchungshilfe/MINIMUM_Buchungshilfe.pdf");

  /** ZUGFeRD 2.4 BASIC WL — latest spec generation; carrier identical to 2.3. */
  public static final File ZF_2_4_BASIC_WL = new File (DOCS,
                                                       "2.4/EN/Examples/1. BASIC WL/BASIC-WL_Einfach/BASIC-WL_Einfach_fx.pdf");

  private SamplePDFs ()
  {}

  /** Skip the calling test (via {@link Assume}) if the sample is not available. */
  public static void assumeAvailable (final File aFile)
  {
    Assume.assumeTrue ("Sample PDF not available: " + aFile, aFile != null && aFile.isFile () && aFile.canRead ());
  }

  /**
   * Walk up from the working directory looking for the docs directory. Falls back to a path
   * relative to the user's working directory.
   */
  private static File _findDocsDirectory ()
  {
    File aCur = new File (System.getProperty ("user.dir")).getAbsoluteFile ();
    for (int i = 0; i < 6 && aCur != null; i++)
    {
      final File aCandidate = new File (aCur, "docs");
      if (aCandidate.isDirectory () && new File (aCandidate, "requirements").isDirectory ())
        return aCandidate;
      aCur = aCur.getParentFile ();
    }
    return new File (System.getProperty ("user.dir"), "docs");
  }
}
