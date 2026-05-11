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
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Aggregated result of a {@link HybridValidator#validate} run. Carries the ordered list of
 * {@link Finding}s and convenience predicates for the caller.
 *
 * @author Philip Helger
 */
@Immutable
public final class ValidationResult
{
  private final ICommonsList <Finding> m_aFindings;

  public ValidationResult (@NonNull final ICommonsList <Finding> aFindings)
  {
    ValueEnforcer.notNull (aFindings, "Findings");
    m_aFindings = aFindings.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <Finding> getAllFindings ()
  {
    return m_aFindings.getClone ();
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <Finding> getFindings (@NonNull final ESeverity eSeverity)
  {
    ValueEnforcer.notNull (eSeverity, "Severity");
    final ICommonsList <Finding> aOut = new CommonsArrayList <> ();
    for (final Finding aF : m_aFindings)
      if (aF.getSeverity () == eSeverity)
        aOut.add (aF);
    return aOut;
  }

  /** @return <code>true</code> iff there is at least one {@link ESeverity#FATAL} finding. */
  public boolean hasFatal ()
  {
    for (final Finding aF : m_aFindings)
      if (aF.getSeverity () == ESeverity.FATAL)
        return true;
    return false;
  }

  /** @return <code>true</code> iff there is at least one {@link ESeverity#WARNING} finding. */
  public boolean hasWarning ()
  {
    for (final Finding aF : m_aFindings)
      if (aF.getSeverity () == ESeverity.WARNING)
        return true;
    return false;
  }

  /** @return <code>true</code> iff no fatal findings were recorded. */
  public boolean isValid ()
  {
    return !hasFatal ();
  }

  public int getFindingCount ()
  {
    return m_aFindings.size ();
  }

  /**
   * Find the first finding with the given rule identifier.
   *
   * @param sRuleID
   *        the rule identifier to look for. May not be <code>null</code>.
   * @return the first matching finding, or <code>null</code>.
   */
  @Nullable
  public Finding findByRuleID (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final Finding aF : m_aFindings)
      if (sRuleID.equals (aF.getRuleID ()))
        return aF;
    return null;
  }

  /**
   * @param sRuleID
   *        the rule identifier to look for. May not be <code>null</code>.
   * @return <code>true</code> if at least one finding matches the given rule identifier.
   */
  public boolean hasRule (@NonNull final String sRuleID)
  {
    return findByRuleID (sRuleID) != null;
  }

  /**
   * @param sRuleID
   *        the rule identifier to look for. May not be <code>null</code>.
   * @return <code>true</code> if at least one finding matches and has {@link ESeverity#FATAL} severity.
   */
  public boolean hasFatalRule (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final Finding aF : m_aFindings)
      if (aF.getSeverity () == ESeverity.FATAL && sRuleID.equals (aF.getRuleID ()))
        return true;
    return false;
  }
}
