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
package com.helger.kaltblut.core.source;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.CGlobal;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Immutable byte / count ceilings applied while reading a hybrid invoice. All values are inclusive;
 * a value of {@code -1} disables that specific limit.
 * <p>
 * The defaults are sized for real-world ZUGFeRD / Factur-X invoices (rarely above a few MB) with
 * comfortable headroom. Callers that knowingly process larger documents can supply a custom
 * instance or use {@link #UNLIMITED}.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridLimits
{
  /**
   * Default maximum size of the input PDF, in bytes (64 MiB).
   */
  public static final long DEFAULT_MAX_PDF_BYTES = 64L * CGlobal.BYTES_PER_MEGABYTE;

  /**
   * Default maximum size of one embedded attachment after decompression, in bytes (32 MiB).
   */
  public static final long DEFAULT_MAX_ATTACHMENT_BYTES = 32L * CGlobal.BYTES_PER_MEGABYTE;

  /**
   * Default maximum aggregate size of all embedded attachments after decompression, in bytes (128
   * MiB).
   */
  public static final long DEFAULT_MAX_AGGREGATE_ATTACHMENT_BYTES = 128L * CGlobal.BYTES_PER_MEGABYTE;

  /**
   * Default maximum number of embedded attachments.
   */
  public static final int DEFAULT_MAX_ATTACHMENT_COUNT = 100;

  /** The default limits. Suitable for unattended invoice processing. */
  @NonNull
  public static final HybridLimits DEFAULTS = new HybridLimits (DEFAULT_MAX_PDF_BYTES,
                                                                DEFAULT_MAX_ATTACHMENT_BYTES,
                                                                DEFAULT_MAX_AGGREGATE_ATTACHMENT_BYTES,
                                                                DEFAULT_MAX_ATTACHMENT_COUNT);

  /** No limits at all. Disables every protection — use only for trusted input. */
  @NonNull
  public static final HybridLimits UNLIMITED = new HybridLimits (-1L, -1L, -1L, -1);

  private final long m_nMaxPdfBytes;
  private final long m_nMaxAttachmentBytes;
  private final long m_nMaxAggregateAttachmentBytes;
  private final int m_nMaxAttachmentCount;

  /**
   * Create a custom set of limits. Use {@code -1} for any value to disable that specific limit.
   *
   * @param nMaxPdfBytes
   *        maximum input PDF size in bytes, or {@code -1} for no limit.
   * @param nMaxAttachmentBytes
   *        maximum per-attachment inflated size in bytes, or {@code -1} for no limit.
   * @param nMaxAggregateAttachmentBytes
   *        maximum aggregate inflated size across all attachments in bytes, or {@code -1} for no
   *        limit.
   * @param nMaxAttachmentCount
   *        maximum number of attachments, or {@code -1} for no limit.
   */
  public HybridLimits (final long nMaxPdfBytes,
                       final long nMaxAttachmentBytes,
                       final long nMaxAggregateAttachmentBytes,
                       final int nMaxAttachmentCount)
  {
    m_nMaxPdfBytes = nMaxPdfBytes < 0 ? -1L : nMaxPdfBytes;
    m_nMaxAttachmentBytes = nMaxAttachmentBytes < 0 ? -1L : nMaxAttachmentBytes;
    m_nMaxAggregateAttachmentBytes = nMaxAggregateAttachmentBytes < 0 ? -1L : nMaxAggregateAttachmentBytes;
    m_nMaxAttachmentCount = nMaxAttachmentCount < 0 ? -1 : nMaxAttachmentCount;
  }

  /**
   * @return the maximum input PDF size in bytes, or {@code -1} for no limit.
   */
  public long getMaxPdfBytes ()
  {
    return m_nMaxPdfBytes;
  }

  /**
   * @return the maximum per-attachment inflated size in bytes, or {@code -1} for no limit.
   */
  public long getMaxAttachmentBytes ()
  {
    return m_nMaxAttachmentBytes;
  }

  /**
   * @return the maximum aggregate inflated size across all attachments in bytes, or {@code -1} for
   *         no limit.
   */
  public long getMaxAggregateAttachmentBytes ()
  {
    return m_nMaxAggregateAttachmentBytes;
  }

  /**
   * @return the maximum number of attachments, or {@code -1} for no limit.
   */
  public int getMaxAttachmentCount ()
  {
    return m_nMaxAttachmentCount;
  }

  /**
   * Return a copy with a different maximum PDF size. Use {@code -1} for no limit.
   *
   * @param nMaxPdfBytes
   *        new value.
   * @return a new instance.
   */
  @NonNull
  public HybridLimits withMaxPdfBytes (final long nMaxPdfBytes)
  {
    return new HybridLimits (nMaxPdfBytes,
                             m_nMaxAttachmentBytes,
                             m_nMaxAggregateAttachmentBytes,
                             m_nMaxAttachmentCount);
  }

  /**
   * Return a copy with a different maximum per-attachment size. Use {@code -1} for no limit.
   *
   * @param nMaxAttachmentBytes
   *        new value.
   * @return a new instance.
   */
  @NonNull
  public HybridLimits withMaxAttachmentBytes (final long nMaxAttachmentBytes)
  {
    return new HybridLimits (m_nMaxPdfBytes,
                             nMaxAttachmentBytes,
                             m_nMaxAggregateAttachmentBytes,
                             m_nMaxAttachmentCount);
  }

  /**
   * Return a copy with a different maximum aggregate attachment size. Use {@code -1} for no limit.
   *
   * @param nMaxAggregateAttachmentBytes
   *        new value.
   * @return a new instance.
   */
  @NonNull
  public HybridLimits withMaxAggregateAttachmentBytes (final long nMaxAggregateAttachmentBytes)
  {
    return new HybridLimits (m_nMaxPdfBytes,
                             m_nMaxAttachmentBytes,
                             nMaxAggregateAttachmentBytes,
                             m_nMaxAttachmentCount);
  }

  /**
   * Return a copy with a different maximum attachment count. Use {@code -1} for no limit.
   *
   * @param nMaxAttachmentCount
   *        new value.
   * @return a new instance.
   */
  @NonNull
  public HybridLimits withMaxAttachmentCount (final int nMaxAttachmentCount)
  {
    return new HybridLimits (m_nMaxPdfBytes,
                             m_nMaxAttachmentBytes,
                             m_nMaxAggregateAttachmentBytes,
                             nMaxAttachmentCount);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("MaxPdfBytes", m_nMaxPdfBytes)
                                       .append ("MaxAttachmentBytes", m_nMaxAttachmentBytes)
                                       .append ("MaxAggregateAttachmentBytes", m_nMaxAggregateAttachmentBytes)
                                       .append ("MaxAttachmentCount", m_nMaxAttachmentCount)
                                       .getToString ();
  }
}
