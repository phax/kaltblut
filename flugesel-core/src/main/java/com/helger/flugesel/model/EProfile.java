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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Hybrid invoice profile, read from the XMP <code>fx:ConformanceLevel</code> /
 * <code>zf:ConformanceLevel</code> property.
 *
 * @author Philip Helger
 */
public enum EProfile
{
  /** ZUGFeRD 2.0.1+ / Factur-X. Booking aid only; no invoice lines. */
  MINIMUM ("MINIMUM"),
  /** Header + footer only; no invoice lines. */
  BASIC_WL ("BASIC WL"),
  /** Subset of EN 16931 with lines. */
  BASIC ("BASIC"),
  /** ZUGFeRD 1.0 / 2.0.1 name for the EN 16931-compliant core profile. */
  COMFORT ("COMFORT"),
  /** Factur-X / ZUGFeRD 2.1+ EN 16931-compliant core profile. */
  EN_16931 ("EN 16931"),
  /** Extension beyond EN 16931 for complex business processes. */
  EXTENDED ("EXTENDED"),
  /** XRECHNUNG reference profile (added in ZUGFeRD 2.2). */
  XRECHNUNG ("XRECHNUNG");

  private final String m_sID;

  EProfile (final String sID)
  {
    m_sID = sID;
  }

  /**
   * @return the canonical identifier as it appears in <code>fx:ConformanceLevel</code> /
   *         <code>zf:ConformanceLevel</code>.
   */
  @NonNull
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Look up a profile by the value of <code>fx:ConformanceLevel</code> /
   * <code>zf:ConformanceLevel</code>. Matches canonical identifier first, then case-insensitive,
   * then with whitespace removed (tolerating "EN16931" as a known alias for "EN 16931").
   *
   * @param sID
   *        the identifier to look up. May be <code>null</code>.
   * @return the profile or <code>null</code> if not recognised.
   */
  @Nullable
  public static EProfile getFromIDOrNull (@Nullable final String sID)
  {
    if (sID == null)
      return null;
    for (final EProfile e : values ())
      if (e.m_sID.equals (sID))
        return e;
    // Case-insensitive fallback
    for (final EProfile e : values ())
      if (e.m_sID.equalsIgnoreCase (sID))
        return e;
    // Whitespace-removed fallback ("EN16931" → "EN 16931")
    final String sStripped = sID.replace (" ", "").replace ("_", "");
    for (final EProfile e : values ())
      if (e.m_sID.replace (" ", "").equalsIgnoreCase (sStripped))
        return e;
    return null;
  }
}
