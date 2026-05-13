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

import com.helger.diagnostics.error.level.EErrorLevel;

/**
 * Severity classification for {@link HybridFinding}s. Matches the levels used by the Factur-X
 * BR-HYBRID-* rule block. Each entry carries the equivalent ph-commons {@link EErrorLevel} so
 * consumers can map findings into ph-commons error infrastructure without translation tables.
 *
 * @author Philip Helger
 */
public enum EHybridSeverity
{
  /** Informational finding, no impact on validity. */
  INFORMATION (EErrorLevel.INFO),
  /** Warning: indicates a deviation that does not invalidate the invoice. */
  WARNING (EErrorLevel.WARN),
  /** Error: the invoice does not comply with the specification. */
  ERROR (EErrorLevel.ERROR);

  private final EErrorLevel m_eErrorLevel;

  EHybridSeverity (@NonNull final EErrorLevel eErrorLevel)
  {
    m_eErrorLevel = eErrorLevel;
  }

  /**
   * @return the equivalent ph-commons {@link EErrorLevel} for this severity. Never
   *         <code>null</code>.
   */
  @NonNull
  public EErrorLevel getErrorLevel ()
  {
    return m_eErrorLevel;
  }

  public boolean isError ()
  {
    return this == ERROR;
  }
}
