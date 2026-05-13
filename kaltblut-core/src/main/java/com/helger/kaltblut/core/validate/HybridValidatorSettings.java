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
package com.helger.kaltblut.core.validate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.source.HybridLimits;

/**
 * Mutable settings for {@link HybridValidator}. All settings default to values appropriate for a
 * generic, country-agnostic invoice.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class HybridValidatorSettings
{
  private EZugferdCountry m_eCountry = EZugferdCountry.OTHER;
  private EZugferdFlavor m_eExpectedFlavor;
  private boolean m_bCheckPdfA3 = true;
  private boolean m_bApplyDePdfADowngrade = true;
  private HybridLimits m_aLimits = HybridLimits.DEFAULTS;

  /**
   * @return the country context. Drives BR-HYBRID-DE-*, BR-HYBRID-FR-*, BR-FX-DE-*. Default is
   *         {@link EZugferdCountry#OTHER}.
   */
  @NonNull
  public EZugferdCountry getCountry ()
  {
    return m_eCountry;
  }

  @NonNull
  public HybridValidatorSettings setCountry (@NonNull final EZugferdCountry eCountry)
  {
    ValueEnforcer.notNull (eCountry, "Country");
    m_eCountry = eCountry;
    return this;
  }

  /**
   * @return the flavor the validator should require, or <code>null</code> to auto-detect from XMP.
   */
  @Nullable
  public EZugferdFlavor getExpectedFlavor ()
  {
    return m_eExpectedFlavor;
  }

  @NonNull
  public HybridValidatorSettings setExpectedFlavor (@Nullable final EZugferdFlavor eExpectedFlavor)
  {
    m_eExpectedFlavor = eExpectedFlavor;
    return this;
  }

  /**
   * @return whether to run PDF/A-3 validation via the {@link IPdfA3ValidatorSPI} SPI. Default
   *         <code>true</code>.
   */
  public boolean isCheckPdfA3 ()
  {
    return m_bCheckPdfA3;
  }

  @NonNull
  public HybridValidatorSettings setCheckPdfA3 (final boolean bCheckPdfA3)
  {
    m_bCheckPdfA3 = bCheckPdfA3;
    return this;
  }

  /**
   * @return whether to apply the <code>BR-FX-DE-03</code> downgrade (PDF/A-3 fatal errors become
   *         warnings when {@link #getCountry()} is {@link EZugferdCountry#DE} and the XML is valid
   *         / extractable). Default <code>true</code>.
   */
  public boolean isApplyDePdfADowngrade ()
  {
    return m_bApplyDePdfADowngrade;
  }

  @NonNull
  public HybridValidatorSettings setApplyDePdfADowngrade (final boolean bApplyDePdfADowngrade)
  {
    m_bApplyDePdfADowngrade = bApplyDePdfADowngrade;
    return this;
  }

  /**
   * @return the byte / count ceilings applied while reading the PDF and its attachments. Default is
   *         {@link HybridLimits#DEFAULTS}.
   */
  @NonNull
  public HybridLimits getLimits ()
  {
    return m_aLimits;
  }

  @NonNull
  public HybridValidatorSettings setLimits (@NonNull final HybridLimits aLimits)
  {
    ValueEnforcer.notNull (aLimits, "Limits");
    m_aLimits = aLimits;
    return this;
  }
}
