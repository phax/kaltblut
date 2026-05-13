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

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * One logical layer of a {@link HybridValidator} run: a {@link EHybridValidationLayerKind kind}, the
 * wall-clock duration of that layer, and its ordered {@link HybridFinding}s.
 *
 * @author Philip Helger
 * @since 0.9.1
 */
@Immutable
public final class HybridValidationLayer
{
  private final EHybridValidationLayerKind m_eKind;
  private final Duration m_aDuration;
  private final ICommonsList <HybridFinding> m_aFindings;

  public HybridValidationLayer (@NonNull final EHybridValidationLayerKind eKind,
                                @NonNull final Duration aDuration,
                                @NonNull final ICommonsList <HybridFinding> aFindings)
  {
    ValueEnforcer.notNull (eKind, "Kind");
    ValueEnforcer.notNull (aDuration, "Duration");
    ValueEnforcer.notNull (aFindings, "Findings");
    m_eKind = eKind;
    m_aDuration = aDuration;
    m_aFindings = aFindings.getClone ();
  }

  @NonNull
  public EHybridValidationLayerKind getKind ()
  {
    return m_eKind;
  }

  /**
   * @return Display name for this layer (e.g. for UI / log output). Equivalent to
   *         <code>getKind().getName()</code>.
   */
  @NonNull
  public String getDisplayName ()
  {
    return m_eKind.getName ();
  }

  /**
   * @return wall-clock duration of this layer's validation execution.
   */
  @NonNull
  public Duration getDuration ()
  {
    return m_aDuration;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridFinding> getAllFindings ()
  {
    return m_aFindings.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridFinding> getFindings (@NonNull final EHybridSeverity eSeverity)
  {
    ValueEnforcer.notNull (eSeverity, "Severity");
    final ICommonsList <HybridFinding> aOut = new CommonsArrayList <> ();
    for (final HybridFinding aF : m_aFindings)
      if (aF.getSeverity () == eSeverity)
        aOut.add (aF);
    return aOut;
  }

  public int getFindingCount ()
  {
    return m_aFindings.size ();
  }

  /**
   * @return <code>true</code> if there is at least one {@link EHybridSeverity#ERROR} finding.
   */
  public boolean hasError ()
  {
    for (final HybridFinding aF : m_aFindings)
      if (aF.getSeverity () == EHybridSeverity.ERROR)
        return true;
    return false;
  }

  /**
   * @return <code>true</code> if there is at least one {@link EHybridSeverity#WARNING} finding.
   */
  public boolean hasWarning ()
  {
    for (final HybridFinding aF : m_aFindings)
      if (aF.getSeverity () == EHybridSeverity.WARNING)
        return true;
    return false;
  }

  /**
   * @return <code>true</code> if no FATAL finding was recorded on this layer.
   */
  public boolean isValid ()
  {
    return !hasError ();
  }

  @Nullable
  public HybridFinding findByRuleID (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final HybridFinding aF : m_aFindings)
      if (sRuleID.equals (aF.getRuleID ()))
        return aF;
    return null;
  }

  public boolean hasRule (@NonNull final String sRuleID)
  {
    return findByRuleID (sRuleID) != null;
  }

  public boolean hasErrorRule (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final HybridFinding aF : m_aFindings)
      if (aF.getSeverity () == EHybridSeverity.ERROR && sRuleID.equals (aF.getRuleID ()))
        return true;
    return false;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Kind", m_eKind)
                                       .append ("Duration", m_aDuration)
                                       .append ("Findings", m_aFindings)
                                       .getToString ();
  }
}
