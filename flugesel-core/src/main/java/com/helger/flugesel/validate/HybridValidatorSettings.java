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
package com.helger.flugesel.validate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.flugesel.model.ECountry;
import com.helger.flugesel.model.EZugferdFlavor;

/**
 * Mutable settings for {@link HybridValidator}. All settings default to values appropriate for a
 * generic, country-agnostic invoice.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class HybridValidatorSettings
{
  private ECountry m_eCountry = ECountry.OTHER;
  private EZugferdFlavor m_eExpectedFlavor;
  private boolean m_bCheckPdfA3 = true;
  private boolean m_bApplyDePdfADowngrade = true;

  /**
   * @return the country context. Drives BR-HYBRID-DE-*, BR-HYBRID-FR-*, BR-FX-DE-*. Default is
   *         {@link ECountry#OTHER}.
   */
  @NonNull
  public ECountry getCountry ()
  {
    return m_eCountry;
  }

  @NonNull
  public HybridValidatorSettings setCountry (@NonNull final ECountry eCountry)
  {
    if (eCountry == null)
      throw new IllegalArgumentException ("country must not be null");
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

  /** @return whether to run PDF/A-3 validation via the {@link IPdfA3Validator} SPI. Default <code>true</code>. */
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
   *         warnings when {@link #getCountry()} is {@link ECountry#DE} and the XML is valid /
   *         extractable). Default <code>true</code>.
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
}
