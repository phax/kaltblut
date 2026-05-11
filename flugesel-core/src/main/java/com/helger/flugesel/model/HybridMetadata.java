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
package com.helger.flugesel.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;

/**
 * Snapshot of all hybrid-invoice-relevant metadata read from a PDF in a single pass:
 * <ul>
 * <li>the XMP extension-schema fields (<code>fx:DocumentType</code>,
 * <code>fx:DocumentFileName</code>, <code>fx:Version</code>, <code>fx:ConformanceLevel</code>),</li>
 * <li>the embedded invoice XML name and {@code /AFRelationship} as found on the PDF Catalog's
 * associated-file array.</li>
 * </ul>
 * <p>
 * Where a property is missing or unrecognised, the corresponding getter returns <code>null</code>.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridMetadata
{
  private final EZugferdFlavor m_eFlavor;
  private final String m_sNamespaceURI;
  private final String m_sXmpDocumentType;
  private final String m_sXmpDocumentFileName;
  private final String m_sXmpVersion;
  private final EProfile m_eProfile;
  private final String m_sRawProfile;
  private final String m_sEmbeddedFileName;
  private final EAFRelationship m_eAFRelationship;
  private final String m_sRawAFRelationship;

  public HybridMetadata (@Nullable final EZugferdFlavor eFlavor,
                         @Nullable final String sNamespaceURI,
                         @Nullable final String sXmpDocumentType,
                         @Nullable final String sXmpDocumentFileName,
                         @Nullable final String sXmpVersion,
                         @Nullable final EProfile eProfile,
                         @Nullable final String sRawProfile,
                         @Nullable final String sEmbeddedFileName,
                         @Nullable final EAFRelationship eAFRelationship,
                         @Nullable final String sRawAFRelationship)
  {
    m_eFlavor = eFlavor;
    m_sNamespaceURI = sNamespaceURI;
    m_sXmpDocumentType = sXmpDocumentType;
    m_sXmpDocumentFileName = sXmpDocumentFileName;
    m_sXmpVersion = sXmpVersion;
    m_eProfile = eProfile;
    m_sRawProfile = sRawProfile;
    m_sEmbeddedFileName = sEmbeddedFileName;
    m_eAFRelationship = eAFRelationship;
    m_sRawAFRelationship = sRawAFRelationship;
  }

  /** @return the detected flavor by namespace URI, or <code>null</code> if no XMP block was found. */
  @Nullable
  public EZugferdFlavor getFlavor ()
  {
    return m_eFlavor;
  }

  /** @return the raw XMP extension-schema namespace URI. */
  @Nullable
  public String getNamespaceURI ()
  {
    return m_sNamespaceURI;
  }

  /** @return the <code>fx:DocumentType</code> / <code>zf:DocumentType</code> value. */
  @Nullable
  public String getXmpDocumentType ()
  {
    return m_sXmpDocumentType;
  }

  /** @return the <code>fx:DocumentFileName</code> / <code>zf:DocumentFileName</code> value. */
  @Nullable
  public String getXmpDocumentFileName ()
  {
    return m_sXmpDocumentFileName;
  }

  /** @return the <code>fx:Version</code> / <code>zf:Version</code> value (e.g. <code>1.0</code>, <code>1p0</code>, <code>2p0</code>). */
  @Nullable
  public String getXmpVersion ()
  {
    return m_sXmpVersion;
  }

  /** @return the resolved profile, or <code>null</code> if not recognised. */
  @Nullable
  public EProfile getProfile ()
  {
    return m_eProfile;
  }

  /** @return the raw <code>fx:ConformanceLevel</code> / <code>zf:ConformanceLevel</code> value. */
  @Nullable
  public String getRawProfile ()
  {
    return m_sRawProfile;
  }

  /** @return the file name on the embedded-file <code>/F</code> entry, or <code>null</code> if there is no associated file. */
  @Nullable
  public String getEmbeddedFileName ()
  {
    return m_sEmbeddedFileName;
  }

  /** @return the resolved <code>/AFRelationship</code>, or <code>null</code>. */
  @Nullable
  public EAFRelationship getAFRelationship ()
  {
    return m_eAFRelationship;
  }

  /** @return the raw <code>/AFRelationship</code> value as a string. */
  @Nullable
  public String getRawAFRelationship ()
  {
    return m_sRawAFRelationship;
  }

  /** @return <code>true</code> if this PDF carries a recognised hybrid invoice signature. */
  public boolean isRecognisedHybridInvoice ()
  {
    return m_eFlavor != null;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return "HybridMetadata[flavor=" +
           m_eFlavor +
           ", profile=" +
           m_eProfile +
           ", embeddedFile=" +
           m_sEmbeddedFileName +
           ", afRelationship=" +
           m_eAFRelationship +
           "]";
  }
}
