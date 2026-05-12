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
package com.helger.flugesel.inspect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.helger.flugesel.model.EAFRelationship;
import com.helger.flugesel.model.EZugferdProfile;
import com.helger.flugesel.model.EZugferdFlavor;
import com.helger.flugesel.model.HybridMetadata;
import com.helger.flugesel.source.HybridSource;
import com.helger.flugesel.testfiles.FlugeselTestFiles;

/**
 * Integration-style tests for {@link HybridInspector} against the representative samples shipped by
 * flugesel-testfiles. One test per distinct PDF-carrier generation.
 *
 * @author Philip Helger
 */
public final class HybridInspectorTest
{
  @Test
  public void test_2_0_1_LegacyNamespace () throws IOException
  {
    final HybridMetadata aMeta = HybridInspector.readMetadata (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_0_1_EN16931));
    assertTrue (aMeta.isRecognisedHybridInvoice ());
    assertEquals (EZugferdFlavor.ZUGFERD_2_0_LEGACY, aMeta.getFlavor ());
    assertEquals ("INVOICE", aMeta.getXmpDocumentType ());
    assertEquals ("zugferd-invoice.xml", aMeta.getXmpDocumentFileName ());
    assertEquals ("zugferd-invoice.xml", aMeta.getEmbeddedFileName ());
    assertEquals (EZugferdProfile.EN_16931, aMeta.getProfile ());
    assertEquals (EAFRelationship.ALTERNATIVE, aMeta.getAFRelationship ());
  }

  @Test
  public void test_2_1_FacturxPrimary () throws IOException
  {
    final HybridMetadata aMeta = HybridInspector.readMetadata (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_1_EN16931_EMBEDDED));
    assertTrue (aMeta.isRecognisedHybridInvoice ());
    assertEquals (EZugferdFlavor.FACTURX_PRIMARY, aMeta.getFlavor ());
    assertEquals ("factur-x.xml", aMeta.getXmpDocumentFileName ());
    assertEquals ("factur-x.xml", aMeta.getEmbeddedFileName ());
    assertEquals (EZugferdProfile.EN_16931, aMeta.getProfile ());
    // 2.1 sample uses Data (recipient side, not the German Alternative pattern).
    assertEquals (EAFRelationship.DATA, aMeta.getAFRelationship ());
  }

  @Test
  public void test_2_2_XRechnungProfileAndFilename () throws IOException
  {
    final HybridMetadata aMeta = HybridInspector.readMetadata (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_2_XRECHNUNG));
    assertEquals (EZugferdFlavor.FACTURX_PRIMARY, aMeta.getFlavor ());
    assertEquals (EZugferdProfile.XRECHNUNG, aMeta.getProfile ());
    // XRECHNUNG profile uses xrechnung.xml instead of factur-x.xml.
    assertEquals ("xrechnung.xml", aMeta.getEmbeddedFileName ());
    assertEquals ("xrechnung.xml", aMeta.getXmpDocumentFileName ());
    // The shipped sample uses an anomalous fx:Version="2.1" — captured here to lock that observation in.
    assertEquals ("2.1", aMeta.getXmpVersion ());
  }

  @Test
  public void test_2_3_MinimumProfileUsesDataRelationship () throws IOException
  {
    final HybridMetadata aMeta = HybridInspector.readMetadata (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_3_MINIMUM));
    assertEquals (EZugferdProfile.MINIMUM, aMeta.getProfile ());
    assertEquals (EAFRelationship.DATA, aMeta.getAFRelationship ());
    assertEquals ("factur-x.xml", aMeta.getEmbeddedFileName ());
  }

  @Test
  public void test_2_4_LatestSpecParses () throws IOException
  {
    final HybridMetadata aMeta = HybridInspector.readMetadata (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_4_BASIC_WL));
    assertEquals (EZugferdFlavor.FACTURX_PRIMARY, aMeta.getFlavor ());
    assertEquals (EZugferdProfile.BASIC_WL, aMeta.getProfile ());
    assertEquals (EAFRelationship.DATA, aMeta.getAFRelationship ());
    assertEquals ("1.0", aMeta.getXmpVersion ());
  }

  @Test
  public void testIsHybridInvoiceShortcut () throws IOException
  {
    assertTrue (HybridInspector.isHybridInvoice (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_0_1_EN16931)));
  }
}
