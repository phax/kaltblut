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
package com.helger.flugesel.core.model;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Description of one embedded file inside a hybrid PDF (the invoice XML or any supporting
 * document).
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridAttachment
{
  private final String m_sName;
  private final String m_sMimeType;
  private final EAFRelationship m_eAFRelationship;
  private final String m_sRawAFRelationship;
  private final OffsetDateTime m_aModDate;
  private final byte [] m_aBytes;
  private final boolean m_bIsInvoiceXml;

  public HybridAttachment (@NonNull final String sName,
                           @Nullable final String sMimeType,
                           @Nullable final EAFRelationship eAFRelationship,
                           @Nullable final String sRawAFRelationship,
                           @Nullable final OffsetDateTime aModDate,
                           @NonNull final byte [] aBytes,
                           final boolean bIsInvoiceXml)
  {
    ValueEnforcer.notNull (sName, "Name");
    ValueEnforcer.notNull (aBytes, "Bytes");
    m_sName = sName;
    m_sMimeType = sMimeType;
    m_eAFRelationship = eAFRelationship;
    m_sRawAFRelationship = sRawAFRelationship;
    m_aModDate = aModDate;
    m_aBytes = aBytes;
    m_bIsInvoiceXml = bIsInvoiceXml;
  }

  @NonNull
  public String getName ()
  {
    return m_sName;
  }

  @Nullable
  public String getMimeType ()
  {
    return m_sMimeType;
  }

  @Nullable
  public EAFRelationship getAFRelationship ()
  {
    return m_eAFRelationship;
  }

  @Nullable
  public String getRawAFRelationship ()
  {
    return m_sRawAFRelationship;
  }

  @Nullable
  public OffsetDateTime getModDate ()
  {
    return m_aModDate;
  }

  /** @return a copy of the file content. */
  @NonNull
  @ReturnsMutableCopy
  public byte [] getBytes ()
  {
    return Arrays.copyOf (m_aBytes, m_aBytes.length);
  }

  public int getSize ()
  {
    return m_aBytes.length;
  }

  /** @return <code>true</code> if this attachment is the hybrid invoice's structured XML file. */
  public boolean isInvoiceXml ()
  {
    return m_bIsInvoiceXml;
  }

  @Override
  @NonNull
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName)
                                       .append ("MimeType", m_sMimeType)
                                       .append ("AFRelationship", m_eAFRelationship)
                                       .append ("RawAFRelationship", m_sRawAFRelationship)
                                       .append ("ModDate", m_aModDate)
                                       .append ("Size", m_aBytes.length)
                                       .append ("IsInvoiceXml", m_bIsInvoiceXml)
                                       .getToString ();
  }
}
