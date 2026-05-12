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
package com.helger.flugesel.core.inspect;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.flugesel.core.model.EZugferdFlavor;
import com.helger.flugesel.core.model.EZugferdProfile;
import com.helger.flugesel.core.model.HybridMetadata;
import com.helger.flugesel.core.pdfbox.HybridDocument;
import com.helger.flugesel.core.source.IHybridSource;

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
   * @return <code>true</code> if recognised, <code>false</code> otherwise (including when the
   *         input is not a valid PDF).
   * @throws IOException
   *         on I/O failure reading the source.
   */
  public static boolean isHybridInvoice (@NonNull final IHybridSource aSource) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    try
    {
      return readMetadata (aSource).isRecognisedHybridInvoice ();
    }
    catch (final IOException ex)
    {
      throw ex;
    }
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
    ValueEnforcer.notNull (aSource, "Source");
    return HybridDocument.withOpenDocument (aSource, HybridDocument::readMetadata);
  }
}
