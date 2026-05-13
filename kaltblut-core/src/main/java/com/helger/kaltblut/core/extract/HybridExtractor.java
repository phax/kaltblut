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
package com.helger.kaltblut.core.extract;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.kaltblut.core.model.HybridAttachment;
import com.helger.kaltblut.core.pdfbox.HybridDocument;
import com.helger.kaltblut.core.source.HybridLimits;
import com.helger.kaltblut.core.source.IHybridSource;

/**
 * Tier 2: extraction of the invoice XML and any supporting embedded files.
 * <p>
 * <b>Security note:</b> the bytes returned by {@link #extractInvoiceXml(IHybridSource)} and
 * {@link #extractAttachment(IHybridSource, String)} come from a potentially untrusted PDF. Callers
 * parsing the XML themselves <em>must</em> configure their XML processor to disable external
 * entities, DTDs, and XInclude (i.e. {@code FEATURE_SECURE_PROCESSING=true} plus
 * {@code disallow-doctype-decl=true}), or use a library — such as {@code phive-rules-zugferd} —
 * that does so by default. Otherwise the extracted bytes can carry XXE payloads that read local
 * files or trigger SSRF in the consuming process.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridExtractor
{
  private HybridExtractor ()
  {}

  /**
   * Extract the structured invoice XML from a hybrid invoice, using {@link HybridLimits#DEFAULTS}.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the XML bytes, or <code>null</code> if no invoice attachment was located.
   * @throws IOException
   *         on I/O / PDF / XMP parsing failure, or if the source / its attachments exceed the
   *         default limits.
   */
  @Nullable
  public static byte [] extractInvoiceXml (@NonNull final IHybridSource aSource) throws IOException
  {
    return extractInvoiceXml (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * Extract the structured invoice XML from a hybrid invoice, enforcing the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>; use {@link HybridLimits#UNLIMITED} to disable.
   * @return the XML bytes, or <code>null</code> if no invoice attachment was located.
   * @throws IOException
   *         on I/O / PDF / XMP parsing failure or limit violation.
   */
  @Nullable
  public static byte [] extractInvoiceXml (@NonNull final IHybridSource aSource, @NonNull final HybridLimits aLimits)
                                                                                                                      throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    return HybridDocument.withOpenDocument (aSource, aLimits, aDoc -> {
      for (final HybridAttachment aAtt : aDoc.listAttachments ())
        if (aAtt.isInvoiceXml ())
          return aAtt.getBytes ();
      return null;
    });
  }

  /**
   * List every embedded file in the PDF (invoice XML included).
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return the attachments. Empty list if the PDF has no embedded files.
   * @throws IOException
   *         on parsing failure or default-limit violation.
   */
  @NonNull
  public static ICommonsList <HybridAttachment> listAttachments (@NonNull final IHybridSource aSource) throws IOException
  {
    return listAttachments (aSource, HybridLimits.DEFAULTS);
  }

  /**
   * List every embedded file in the PDF, enforcing the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the attachments. Empty list if the PDF has no embedded files.
   * @throws IOException
   *         on parsing failure or limit violation.
   */
  @NonNull
  public static ICommonsList <HybridAttachment> listAttachments (@NonNull final IHybridSource aSource,
                                                                 @NonNull final HybridLimits aLimits) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aLimits, "Limits");
    final ICommonsList <HybridAttachment> aResult = HybridDocument.withOpenDocument (aSource,
                                                                                     aLimits,
                                                                                     HybridDocument::listAttachments);
    return aResult != null ? aResult : new CommonsArrayList <> ();
  }

  /**
   * Extract one named attachment by its embedded-file name, using {@link HybridLimits#DEFAULTS}.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param sName
   *        the name to match (e.g. <code>factur-x.xml</code>,
   *        <code>list_of_measurement.xlsx</code>).
   * @return the bytes, or <code>null</code> if no embedded file matches.
   * @throws IOException
   *         on parsing failure or default-limit violation.
   */
  @Nullable
  public static byte [] extractAttachment (@NonNull final IHybridSource aSource, @NonNull final String sName)
                                                                                                              throws IOException
  {
    return extractAttachment (aSource, sName, HybridLimits.DEFAULTS);
  }

  /**
   * Extract one named attachment by its embedded-file name, enforcing the given byte ceilings.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @param sName
   *        the name to match.
   * @param aLimits
   *        the limits. May not be <code>null</code>.
   * @return the bytes, or <code>null</code> if no embedded file matches.
   * @throws IOException
   *         on parsing failure or limit violation.
   */
  @Nullable
  public static byte [] extractAttachment (@NonNull final IHybridSource aSource,
                                           @NonNull final String sName,
                                           @NonNull final HybridLimits aLimits) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (sName, "Name");
    ValueEnforcer.notNull (aLimits, "Limits");
    return HybridDocument.withOpenDocument (aSource, aLimits, aDoc -> {
      for (final HybridAttachment aAtt : aDoc.listAttachments ())
        if (sName.equals (aAtt.getName ()))
          return aAtt.getBytes ();
      return null;
    });
  }
}
