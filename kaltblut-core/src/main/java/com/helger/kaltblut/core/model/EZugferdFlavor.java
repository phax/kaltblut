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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Identifies which XMP extension-schema namespace a hybrid invoice declares.
 * <p>
 * The namespace URI is the most reliable fingerprint for distinguishing ZUGFeRD / Factur-X spec
 * generations. The five URIs in this enum cover every release from ZUGFeRD 1.0 (2014) through
 * Factur-X 1.08 / ZUGFeRD 2.4 (2025-12-04).
 *
 * @author Philip Helger
 */
public enum EZugferdFlavor
{
  /**
   * ZUGFeRD 1.0 (2014). Profiles BASIC, COMFORT, EXTENDED. Embedded XML named
   * <code>ZUGFeRD-invoice.xml</code>.
   */
  ZUGFERD_1 ("urn:ferd:pdfa:CrossIndustryDocument:invoice:1p0#", "zf", "ZUGFeRD-invoice.xml"),

  /**
   * ZUGFeRD 2.0 / 2.0.1 / 2.1 legacy (Supplement B). Embedded XML named
   * <code>zugferd-invoice.xml</code>; URI version segment <code>2p0</code>.
   */
  ZUGFERD_2_0_LEGACY ("urn:zugferd:pdfa:CrossIndustryDocument:invoice:2p0#", "fx", "zugferd-invoice.xml"),

  /**
   * Factur-X primary namespace. Used from ZUGFeRD 2.1 (Supplement A) onward as the preferred
   * identifier. Embedded XML named <code>factur-x.xml</code> (or <code>xrechnung.xml</code> for the
   * XRECHNUNG reference profile).
   */
  FACTURX_PRIMARY ("urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#", "fx", "factur-x.xml"),

  /**
   * Legacy variant tolerated by ZUGFeRD 2.2 onward. Note the version segment regressed from
   * <code>2p0</code> to <code>1p0</code> compared to {@link #ZUGFERD_2_0_LEGACY}.
   */
  ZUGFERD_2_2_LEGACY ("urn:zugferd:pdfa:CrossIndustryDocument:invoice:1p0#", "zf", "zugferd-invoice.xml"),

  /**
   * BR-HYBRID-04 wording from spec 2.3.2 onward. Apparent transcription error vs §6.3.1 (missing
   * <code>:invoice</code> segment). Treated as a recognised but irregular variant.
   */
  FACTURX_BR_HYBRID_04 ("urn:factur-x:pdfa:CrossIndustryDocument:1p0#", "fx", "factur-x.xml");

  private final String m_sNamespaceURI;
  private final String m_sExpectedPrefix;
  private final String m_sDefaultEmbeddedFileName;

  EZugferdFlavor (final String sNamespaceURI, final String sExpectedPrefix, final String sDefaultEmbeddedFileName)
  {
    m_sNamespaceURI = sNamespaceURI;
    m_sExpectedPrefix = sExpectedPrefix;
    m_sDefaultEmbeddedFileName = sDefaultEmbeddedFileName;
  }

  /**
   * @return the XMP extension-schema namespace URI for this flavor (including the trailing
   *         <code>#</code>).
   */
  @NonNull
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  /**
   * @return the XMP namespace prefix expected by this flavor (e.g. <code>fx</code> or
   *         <code>zf</code>).
   */
  @NonNull
  public String getExpectedPrefix ()
  {
    return m_sExpectedPrefix;
  }

  /**
   * @return the default embedded XML file name for this flavor. For Factur-X primary the XRECHNUNG
   *         profile overrides this to <code>xrechnung.xml</code>.
   */
  @NonNull
  public String getDefaultEmbeddedFileName ()
  {
    return m_sDefaultEmbeddedFileName;
  }

  /**
   * Look up a flavor by its XMP extension-schema namespace URI.
   *
   * @param sNamespaceURI
   *        the URI to match. May be <code>null</code>.
   * @return the flavor or <code>null</code> if no known flavor matches.
   */
  @Nullable
  public static EZugferdFlavor getFromNamespaceURIOrNull (@Nullable final String sNamespaceURI)
  {
    if (sNamespaceURI != null)
      for (final EZugferdFlavor e : values ())
        if (e.m_sNamespaceURI.equals (sNamespaceURI))
          return e;
    return null;
  }
}
