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
package com.helger.flugesel.core.extract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.collection.commons.ICommonsList;
import com.helger.flugesel.core.model.HybridAttachment;
import com.helger.flugesel.core.source.HybridSource;
import com.helger.flugesel.testfiles.FlugeselTestFiles;

public final class HybridExtractorTest
{
  @Test
  public void testExtractInvoiceXml_LegacyFileName () throws IOException
  {
    final byte [] aXml = HybridExtractor.extractInvoiceXml (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_0_1_EN16931));
    assertNotNull (aXml);
    assertTrue ("Expected non-empty XML", aXml.length > 0);
    // PDFBox returns the embedded bytes verbatim; XMLs from FeRD start with a UTF-8 XML declaration.
    final String sHead = new String (aXml, 0, Math.min (aXml.length, 32), StandardCharsets.UTF_8);
    assertTrue ("Embedded XML must start with an XML declaration: " + sHead, sHead.startsWith ("<?xml"));
  }

  @Test
  public void testExtractInvoiceXml_FacturXFileName () throws IOException
  {
    final byte [] aXml = HybridExtractor.extractInvoiceXml (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_4_BASIC_WL));
    assertNotNull (aXml);
    assertTrue (aXml.length > 0);
  }

  @Test
  public void testExtractInvoiceXml_XRechnungFileName () throws IOException
  {
    final byte [] aXml = HybridExtractor.extractInvoiceXml (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_2_XRECHNUNG));
    assertNotNull (aXml);
    assertTrue (aXml.length > 0);
  }

  @Test
  public void testListAttachments_InvoiceFlaggedCorrectly () throws IOException
  {
    final ICommonsList <HybridAttachment> aAtts = HybridExtractor.listAttachments (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_0_1_EN16931));
    assertEquals (1, aAtts.size ());
    final HybridAttachment aAtt = aAtts.get (0);
    assertEquals ("zugferd-invoice.xml", aAtt.getName ());
    assertEquals ("text/xml", aAtt.getMimeType ());
    assertTrue ("Invoice flag must be set", aAtt.isInvoiceXml ());
    assertTrue ("Attachment must have bytes", aAtt.getSize () > 0);
  }

  @Test
  public void testExtractAttachmentByName () throws IOException
  {
    final byte [] aBytes = HybridExtractor.extractAttachment (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_2_XRECHNUNG),
                                                              "xrechnung.xml");
    assertNotNull (aBytes);
    assertTrue (aBytes.length > 0);
  }

  @Test
  public void testExtractAttachmentByName_NotFound () throws IOException
  {
    final byte [] aBytes = HybridExtractor.extractAttachment (HybridSource.fromClasspath (FlugeselTestFiles.ZF_2_0_1_EN16931),
                                                              "does-not-exist.xml");
    assertNull (aBytes);
  }
}
