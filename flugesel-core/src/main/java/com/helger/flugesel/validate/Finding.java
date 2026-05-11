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

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;

/**
 * A single validation finding produced by {@link HybridValidator}.
 *
 * @author Philip Helger
 */
@Immutable
public final class Finding
{
  private final String m_sRuleID;
  private final ESeverity m_eSeverity;
  private final String m_sMessage;
  private final String m_sLocation;

  public Finding (@NonNull final String sRuleID,
                  @NonNull final ESeverity eSeverity,
                  @NonNull final String sMessage,
                  @Nullable final String sLocation)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    ValueEnforcer.notNull (eSeverity, "Severity");
    ValueEnforcer.notNull (sMessage, "Message");
    m_sRuleID = sRuleID;
    m_eSeverity = eSeverity;
    m_sMessage = sMessage;
    m_sLocation = sLocation;
  }

  @NonNull
  public String getRuleID ()
  {
    return m_sRuleID;
  }

  @NonNull
  public ESeverity getSeverity ()
  {
    return m_eSeverity;
  }

  @NonNull
  public String getMessage ()
  {
    return m_sMessage;
  }

  @Nullable
  public String getLocation ()
  {
    return m_sLocation;
  }

  /** Return a copy of this finding with a downgraded severity. */
  @NonNull
  public Finding withSeverity (@NonNull final ESeverity eNewSeverity)
  {
    ValueEnforcer.notNull (eNewSeverity, "NewSeverity");
    return new Finding (m_sRuleID, eNewSeverity, m_sMessage, m_sLocation);
  }

  @Override
  @NonNull
  public String toString ()
  {
    return "[" + m_eSeverity + "] " + m_sRuleID + ": " + m_sMessage + (m_sLocation == null ? "" : " (" + m_sLocation + ")");
  }
}
