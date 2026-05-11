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

public final class EProfileTest
{
  @Test
  public void testCanonicalIDs ()
  {
    assertEquals (EProfile.MINIMUM, EProfile.getFromIDOrNull ("MINIMUM"));
    assertEquals (EProfile.BASIC_WL, EProfile.getFromIDOrNull ("BASIC WL"));
    assertEquals (EProfile.BASIC, EProfile.getFromIDOrNull ("BASIC"));
    assertEquals (EProfile.EN_16931, EProfile.getFromIDOrNull ("EN 16931"));
    assertEquals (EProfile.EXTENDED, EProfile.getFromIDOrNull ("EXTENDED"));
    assertEquals (EProfile.XRECHNUNG, EProfile.getFromIDOrNull ("XRECHNUNG"));
    assertEquals (EProfile.COMFORT, EProfile.getFromIDOrNull ("COMFORT"));
  }

  /** "EN16931" (no space) is a known alias permitted in implementations per 2.1 Supplement A. */
  @Test
  public void testEn16931WhitespaceAlias ()
  {
    assertEquals (EProfile.EN_16931, EProfile.getFromIDOrNull ("EN16931"));
  }

  @Test
  public void testBasicWlUnderscoreAlias ()
  {
    // Some implementations use "BASIC_WL" with an underscore.
    assertEquals (EProfile.BASIC_WL, EProfile.getFromIDOrNull ("BASIC_WL"));
  }

  @Test
  public void testCaseInsensitive ()
  {
    assertEquals (EProfile.EXTENDED, EProfile.getFromIDOrNull ("extended"));
    assertEquals (EProfile.XRECHNUNG, EProfile.getFromIDOrNull ("xrechnung"));
  }

  @Test
  public void testNullAndUnknown ()
  {
    assertNull (EProfile.getFromIDOrNull (null));
    assertNull (EProfile.getFromIDOrNull (""));
    assertNull (EProfile.getFromIDOrNull ("FOO"));
  }
}
