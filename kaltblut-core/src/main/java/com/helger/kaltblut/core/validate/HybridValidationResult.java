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

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * Aggregated result of a {@link HybridValidator#validate} run. Carries the ordered list of
 * {@link HybridValidationLayer}s (typically {@link EHybridValidationLayerKind#BR_HYBRID} followed
 * by {@link EHybridValidationLayerKind#PDF_A3} when enabled). Convenience predicates aggregate
 * findings across all layers.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridValidationResult
{
  private final ICommonsList <HybridValidationLayer> m_aLayers;

  public HybridValidationResult (@NonNull final ICommonsList <HybridValidationLayer> aLayers)
  {
    ValueEnforcer.notNull (aLayers, "Layers");
    m_aLayers = aLayers.getClone ();
  }

  /** @return the layers in execution order. */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridValidationLayer> getAllLayers ()
  {
    return m_aLayers.getClone ();
  }

  /**
   * @param eKind
   *        the layer kind to look for. May not be <code>null</code>.
   * @return the first layer of that kind, or <code>null</code> if no such layer was produced.
   */
  @Nullable
  public HybridValidationLayer getLayer (@NonNull final EHybridValidationLayerKind eKind)
  {
    ValueEnforcer.notNull (eKind, "Kind");
    for (final HybridValidationLayer aLayer : m_aLayers)
      if (aLayer.getKind () == eKind)
        return aLayer;
    return null;
  }

  /**
   * @return the flattened list of findings across all layers.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridFinding> getAllFindings ()
  {
    final ICommonsList <HybridFinding> aOut = new CommonsArrayList <> ();
    for (final HybridValidationLayer aLayer : m_aLayers)
      aOut.addAll (aLayer.getAllFindings ());
    return aOut;
  }

  /**
   * @param eSeverity
   *        The severity to search. May not be <code>null</code>.
   * @return the findings of the given severity across all layers.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <HybridFinding> getFindings (@NonNull final EHybridSeverity eSeverity)
  {
    ValueEnforcer.notNull (eSeverity, "Severity");
    final ICommonsList <HybridFinding> aOut = new CommonsArrayList <> ();
    for (final HybridValidationLayer aLayer : m_aLayers)
      for (final HybridFinding aF : aLayer.getAllFindings ())
        if (aF.getSeverity () == eSeverity)
          aOut.add (aF);
    return aOut;
  }

  /**
   * @return <code>true</code> if there is at least one {@link EHybridSeverity#ERROR} finding.
   */
  public boolean hasError ()
  {
    for (final HybridValidationLayer aLayer : m_aLayers)
      if (aLayer.hasError ())
        return true;
    return false;
  }

  /** @return <code>true</code> if there is at least one {@link EHybridSeverity#WARNING} finding. */
  public boolean hasWarning ()
  {
    for (final HybridValidationLayer aLayer : m_aLayers)
      if (aLayer.hasWarning ())
        return true;
    return false;
  }

  /** @return <code>true</code> if no error findings were recorded across all layers. */
  public boolean isValid ()
  {
    return !hasError ();
  }

  public int getFindingCount ()
  {
    int n = 0;
    for (final HybridValidationLayer aLayer : m_aLayers)
      n += aLayer.getFindingCount ();
    return n;
  }

  /**
   * Find the first finding with the given rule identifier across all layers.
   *
   * @param sRuleID
   *        the rule identifier to look for. May not be <code>null</code>.
   * @return the first matching finding, or <code>null</code>.
   */
  @Nullable
  public HybridFinding findByRuleID (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final HybridValidationLayer aLayer : m_aLayers)
    {
      final HybridFinding aF = aLayer.findByRuleID (sRuleID);
      if (aF != null)
        return aF;
    }
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
   * @return <code>true</code> if at least one finding matches and has {@link EHybridSeverity#ERROR}
   *         severity.
   */
  public boolean hasErrorRule (@NonNull final String sRuleID)
  {
    ValueEnforcer.notNull (sRuleID, "RuleID");
    for (final HybridValidationLayer aLayer : m_aLayers)
      if (aLayer.hasErrorRule (sRuleID))
        return true;
    return false;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Layers", m_aLayers).getToString ();
  }
}
