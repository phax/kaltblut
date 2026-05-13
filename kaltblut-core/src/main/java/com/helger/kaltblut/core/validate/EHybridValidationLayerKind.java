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

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasName;

/**
 * The kind of validation layer produced by {@link HybridValidator}. Each
 * {@link HybridValidationLayer} has exactly one kind. Used by consumers that want to report
 * findings per logical concern (BR-HYBRID business rules vs. PDF/A-3 conformance) rather than as a
 * flat list.
 *
 * @author Philip Helger
 * @since 0.9.1
 */
public enum EHybridValidationLayerKind implements IHasID <String>, IHasName
{
  /**
   * BR-HYBRID-* business rules: XMP / AF / embedded-file checks defined by Factur-X / ZUGFeRD.
   */
  BR_HYBRID ("br-hybrid", "BR-HYBRID rules"),

  /**
   * PDF/A-3 carrier conformance, executed via the {@link IPdfA3ValidatorSPI}. When no SPI is
   * registered the layer exists but contains a single informational finding.
   */
  PDF_A3 ("pdf-a3", "PDF/A-3 conformance");

  private final String m_sID;
  private final String m_sName;

  EHybridValidationLayerKind (@NonNull final String sID, @NonNull final String sName)
  {
    m_sID = sID;
    m_sName = sName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @Nullable
  public static EHybridValidationLayerKind getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EHybridValidationLayerKind.class, sID);
  }
}
