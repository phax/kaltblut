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
package com.helger.kaltblut.core.inspect;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.kaltblut.core.model.EZugferdFlavor;
import com.helger.kaltblut.core.model.EZugferdProfile;
import com.helger.kaltblut.core.model.HybridMetadata;
import com.helger.kaltblut.core.pdfbox.HybridDocument;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.IHybridSource;

/**
 * Tier 1: detection and metadata reading for hybrid invoices.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridInspector
{
  private HybridInspector ()
  {}

  /**
   * Quick check whether the given PDF carries a recognised hybrid-invoice XMP signature. Equivalent
   * to checking {@code readMetadata(...).isRecognisedHybridInvoice()}.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return <code>true</code> if recognised, <code>false</code> otherwise (including when the input
   *         is not a valid PDF).
   * @throws IOException
   *         on I/O failure reading the source.
   */
  public static boolean isHybridInvoice (@NonNull final IHybridSource aSource) throws IOException
  {
    return isHybridInvoice (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * Quick check whether the given PDF carries a recognised hybrid-invoice XMP signature, enforcing
   * the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return <code>true</code> if recognised, <code>false</code> otherwise.
   * @throws IOException
   *         on I/O failure or limit violation.
   */
  public static boolean isHybridInvoice (@NonNull final IHybridSource aSource, @NonNull final HybridLimits aLimits)
                                                                                                                    throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    return readMetadata (aSource, aLimits).isRecognisedHybridInvoice ();
  }

  /**
   * Identify the spec generation (namespace URI) of a hybrid invoice.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the detected flavor, or <code>null</code> if no recognised XMP signature is found.
   * @throws IOException
   *         on I/O failure.
   */
  @Nullable
  public static EZugferdFlavor detectFlavor (@NonNull final IHybridSource aSource) throws IOException
  {
    return readMetadata (aSource).getFlavor ();
  }

  /**
   * Identify the spec generation (namespace URI) of a hybrid invoice, enforcing the given byte
   * ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the detected flavor, or <code>null</code> if no recognised XMP signature is found.
   * @throws IOException
   *         on I/O failure or limit violation.
   */
  @Nullable
  public static EZugferdFlavor detectFlavor (@NonNull final IHybridSource aSource, @NonNull final HybridLimits aLimits)
                                                                                                                        throws IOException
  {
    return readMetadata (aSource, aLimits).getFlavor ();
  }

  /**
   * Read the resolved profile.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the resolved profile, or <code>null</code> if not present / not recognised.
   * @throws IOException
   *         on I/O failure.
   */
  @Nullable
  public static EZugferdProfile detectProfile (@NonNull final IHybridSource aSource) throws IOException
  {
    return readMetadata (aSource).getProfile ();
  }

  /**
   * Read the resolved profile, enforcing the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the resolved profile, or <code>null</code>.
   * @throws IOException
   *         on I/O failure or limit violation.
   */
  @Nullable
  public static EZugferdProfile detectProfile (@NonNull final IHybridSource aSource,
                                               @NonNull final HybridLimits aLimits) throws IOException
  {
    return readMetadata (aSource, aLimits).getProfile ();
  }

  /**
   * Read all hybrid-invoice metadata in a single pass.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the metadata snapshot.
   * @throws IOException
   *         on I/O / PDF / XMP parsing failure.
   */
  @NonNull
  public static HybridMetadata readMetadata (@NonNull final IHybridSource aSource) throws IOException
  {
    return readMetadata (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * Read all hybrid-invoice metadata in a single pass, enforcing the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the metadata snapshot.
   * @throws IOException
   *         on I/O / PDF / XMP parsing failure or limit violation.
   */
  @NonNull
  public static HybridMetadata readMetadata (@NonNull final IHybridSource aSource, @NonNull final HybridLimits aLimits)
                                                                                                                        throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    return HybridDocument.withOpenDocument (aSource, aLimits, HybridDocument::readMetadata);
  }
}
