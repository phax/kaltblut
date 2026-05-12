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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * PDF/A-3 <code>AFRelationship</code> values as used by ZUGFeRD / Factur-X for the embedded invoice
 * XML.
 *
 * @author Philip Helger
 */
public enum EAFRelationship implements IHasID <String>
{
  /** XML carries data behind the visual rendering (used for MINIMUM / BASIC WL). */
  DATA ("Data"),
  /** PDF was rendered from the XML; XML is the authoritative source. */
  SOURCE ("Source"),
  /**
   * PDF and XML are equivalent representations (required in Germany for BASIC, EN 16931, EXTENDED,
   * XRECHNUNG).
   */
  ALTERNATIVE ("Alternative"),
  /** Additional information, neither source nor alternative. */
  SUPPLEMENT ("Supplement"),
  /** Relationship unknown or not declared. */
  UNSPECIFIED ("Unspecified");

  private final String m_sID;

  EAFRelationship (final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static EAFRelationship getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDCaseInsensitiveOrNull (EAFRelationship.class, sID);
  }
}
