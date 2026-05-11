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
package com.helger.flugesel.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class EZugferdFlavorTest
{
  @Test
  public void testKnownNamespaceURIs ()
  {
    assertEquals (EZugferdFlavor.ZUGFERD_1,
                  EZugferdFlavor.getFromNamespaceURI ("urn:ferd:pdfa:CrossIndustryDocument:invoice:1p0#"));
    assertEquals (EZugferdFlavor.ZUGFERD_2_0_LEGACY,
                  EZugferdFlavor.getFromNamespaceURI ("urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#"));
    assertEquals (EZugferdFlavor.FACTURX_PRIMARY,
                  EZugferdFlavor.getFromNamespaceURI ("urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#"));
    assertEquals (EZugferdFlavor.ZUGFERD_2_2_LEGACY,
                  EZugferdFlavor.getFromNamespaceURI ("urn:zugferd:pdfa:CrossIndustryDocument:invoice:1p0#"));
    assertEquals (EZugferdFlavor.FACTURX_BR_HYBRID_04,
                  EZugferdFlavor.getFromNamespaceURI ("urn:factur-x:pdfa:CrossIndustryDocument:1p0#"));
  }

  @Test
  public void testUnknownAndNull ()
  {
    assertNull (EZugferdFlavor.getFromNamespaceURI (null));
    assertNull (EZugferdFlavor.getFromNamespaceURI (""));
    assertNull (EZugferdFlavor.getFromNamespaceURI ("urn:unknown:something#"));
  }

  @Test
  public void testDefaultEmbeddedFileNames ()
  {
    assertEquals ("ZUGFeRD-invoice.xml", EZugferdFlavor.ZUGFERD_1.getDefaultEmbeddedFileName ());
    assertEquals ("zugferd-invoice.xml", EZugferdFlavor.ZUGFERD_2_0_LEGACY.getDefaultEmbeddedFileName ());
    assertEquals ("zugferd-invoice.xml", EZugferdFlavor.ZUGFERD_2_2_LEGACY.getDefaultEmbeddedFileName ());
    assertEquals ("factur-x.xml", EZugferdFlavor.FACTURX_PRIMARY.getDefaultEmbeddedFileName ());
  }

  @Test
  public void testPrefixes ()
  {
    assertEquals ("zf", EZugferdFlavor.ZUGFERD_1.getExpectedPrefix ());
    assertEquals ("fx", EZugferdFlavor.ZUGFERD_2_0_LEGACY.getExpectedPrefix ());
    assertEquals ("zf", EZugferdFlavor.ZUGFERD_2_2_LEGACY.getExpectedPrefix ());
    assertEquals ("fx", EZugferdFlavor.FACTURX_PRIMARY.getExpectedPrefix ());
  }
}
