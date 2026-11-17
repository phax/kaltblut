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
package com.helger.kaltblut.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for class {@link HybridXmlContext}.
 *
 * @author Alexandre Gabriel Carvalho Silva
 */
public final class HybridXmlContextTest
{
  @Test
  public void testBuilderDefaults ()
  {
    final HybridXmlContext aCtx = HybridXmlContext.builder ().build ();
    assertNull (aCtx.getSyntax ());
    assertNull (aCtx.getBuyerCountryCode ());
    assertEquals (EZugferdCountry.OTHER, aCtx.getResolvedCountry ());
    assertEquals (EZugferdCountry.OTHER, aCtx.getResolvedBuyerCountry ());
  }

  @Test
  public void testBuilder ()
  {
    final HybridXmlContext aCtx = HybridXmlContext.builder ()
                                                  .syntax (HybridXmlContext.SYNTAX_CII)
                                                  .customizationID ("urn:cen.eu:en16931:2017")
                                                  .businessProcessType ("A1")
                                                  .sellerVatCountryCode ("DE")
                                                  .sellerPostalCountryCode ("DE")
                                                  .resolvedCountry (EZugferdCountry.DE)
                                                  .buyerPostalCountryCode ("FR")
                                                  .resolvedBuyerCountry (EZugferdCountry.FR)
                                                  .build ();
    assertEquals (HybridXmlContext.SYNTAX_CII, aCtx.getSyntax ());
    assertEquals ("urn:cen.eu:en16931:2017", aCtx.getCustomizationID ());
    assertEquals ("A1", aCtx.getBusinessProcessType ());
    assertEquals (EZugferdCountry.DE, aCtx.getResolvedCountry ());
    // No buyer VAT prefix, so the postal country is used
    assertEquals ("FR", aCtx.getBuyerCountryCode ());
    assertEquals (EZugferdCountry.FR, aCtx.getResolvedBuyerCountry ());
  }

  @Test (expected = NullPointerException.class)
  public void testBuilderRejectsNullCountry ()
  {
    HybridXmlContext.builder ().resolvedCountry (null);
  }
}
