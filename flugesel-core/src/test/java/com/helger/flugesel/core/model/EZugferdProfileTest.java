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
package com.helger.flugesel.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class EZugferdProfileTest
{
  @Test
  public void testCanonicalIDs ()
  {
    assertEquals (EZugferdProfile.MINIMUM, EZugferdProfile.getFromIDOrNull ("MINIMUM"));
    assertEquals (EZugferdProfile.BASIC_WL, EZugferdProfile.getFromIDOrNull ("BASIC WL"));
    assertEquals (EZugferdProfile.BASIC, EZugferdProfile.getFromIDOrNull ("BASIC"));
    assertEquals (EZugferdProfile.EN_16931, EZugferdProfile.getFromIDOrNull ("EN 16931"));
    assertEquals (EZugferdProfile.EXTENDED, EZugferdProfile.getFromIDOrNull ("EXTENDED"));
    assertEquals (EZugferdProfile.XRECHNUNG, EZugferdProfile.getFromIDOrNull ("XRECHNUNG"));
    assertEquals (EZugferdProfile.COMFORT, EZugferdProfile.getFromIDOrNull ("COMFORT"));
  }

  /** "EN16931" (no space) is a known alias permitted in implementations per 2.1 Supplement A. */
  @Test
  public void testEn16931WhitespaceAlias ()
  {
    assertEquals (EZugferdProfile.EN_16931, EZugferdProfile.getFromIDOrNull ("EN16931"));
  }

  @Test
  public void testBasicWlUnderscoreAlias ()
  {
    // Some implementations use "BASIC_WL" with an underscore.
    assertEquals (EZugferdProfile.BASIC_WL, EZugferdProfile.getFromIDOrNull ("BASIC_WL"));
  }

  @Test
  public void testCaseInsensitive ()
  {
    assertEquals (EZugferdProfile.EXTENDED, EZugferdProfile.getFromIDOrNull ("extended"));
    assertEquals (EZugferdProfile.XRECHNUNG, EZugferdProfile.getFromIDOrNull ("xrechnung"));
  }

  @Test
  public void testNullAndUnknown ()
  {
    assertNull (EZugferdProfile.getFromIDOrNull (null));
    assertNull (EZugferdProfile.getFromIDOrNull (""));
    assertNull (EZugferdProfile.getFromIDOrNull ("FOO"));
  }
}
