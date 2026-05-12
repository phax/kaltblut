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

public final class EAFRelationshipTest
{
  @Test
  public void testCanonical ()
  {
    assertEquals (EAFRelationship.DATA, EAFRelationship.getFromIDOrNull ("Data"));
    assertEquals (EAFRelationship.SOURCE, EAFRelationship.getFromIDOrNull ("Source"));
    assertEquals (EAFRelationship.ALTERNATIVE, EAFRelationship.getFromIDOrNull ("Alternative"));
    assertEquals (EAFRelationship.SUPPLEMENT, EAFRelationship.getFromIDOrNull ("Supplement"));
    assertEquals (EAFRelationship.UNSPECIFIED, EAFRelationship.getFromIDOrNull ("Unspecified"));
  }

  @Test
  public void testCaseInsensitive ()
  {
    // PDF/A-3 keys are case-sensitive, but tolerate odd casing on lookup for robustness.
    assertEquals (EAFRelationship.ALTERNATIVE, EAFRelationship.getFromIDOrNull ("alternative"));
    assertEquals (EAFRelationship.DATA, EAFRelationship.getFromIDOrNull ("DATA"));
  }

  @Test
  public void testNullAndUnknown ()
  {
    assertNull (EAFRelationship.getFromIDOrNull (null));
    assertNull (EAFRelationship.getFromIDOrNull (""));
    assertNull (EAFRelationship.getFromIDOrNull ("Whatever"));
  }
}
