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
package com.helger.flugesel.core.source;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction for a source of ZUGFeRD / Factur-X hybrid invoice bytes (the PDF envelope).
 * <p>
 * The contract is byte-array-centric on purpose: every flugesel operation eventually needs the
 * complete PDF in memory (PDFBox 3 requires random access), so distinguishing single-read from
 * multi-read inputs adds API surface without value. Implementations may read lazily on first call
 * and cache the result.
 * <p>
 * Implementations must return the same content on every call to {@link #getBytes()}. They are not
 * required to return a defensive copy; callers must not modify the returned array.
 *
 * @author Philip Helger
 */
public interface IHybridSource
{
  /**
   * Return the complete PDF bytes.
   *
   * @return the bytes. Never <code>null</code>.
   * @throws IOException
   *         on I/O failure while reading lazily.
   */
  @NonNull
  byte [] getBytes () throws IOException;

  /**
   * @return the size of the source in bytes if known, or <code>-1</code> if not known up front.
   *         Hint only; once {@link #getBytes()} has been called the canonical size is
   *         <code>getBytes().length</code>.
   */
  default long getSize ()
  {
    return -1L;
  }

  /**
   * @return a short human-readable identifier of this source, e.g. a file name or classpath
   *         resource. May be <code>null</code>. Used only for diagnostic messages.
   */
  @Nullable
  default String getName ()
  {
    return null;
  }
}
