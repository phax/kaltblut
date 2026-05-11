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
package com.helger.flugesel.source;

import org.jspecify.annotations.Nullable;

import com.helger.base.io.iface.IHasInputStream;

/**
 * Abstraction for a source of ZUGFeRD / Factur-X hybrid invoice bytes (the PDF envelope).
 * <p>
 * Re-readability is exposed via {@link #isReadMultiple()} inherited from {@link IHasInputStream}.
 * A re-readable source can be passed to multiple flugesel operations (inspect, extract, validate)
 * without re-loading; a single-read source can only be consumed once and callers should either use
 * {@link HybridSource#materialize} up front or compose all operations through a
 * {@link HybridDocument}.
 *
 * @author Philip Helger
 */
public interface IHybridSource extends IHasInputStream
{
  /**
   * @return the size of the source in bytes if known, or <code>-1</code> if not known. Used as a
   *         hint only; consumers must not rely on it being accurate when not known.
   */
  default long getSize ()
  {
    return -1L;
  }

  /**
   * @return a short human-readable identifier of this source, e.g. a file name. May be
   *         <code>null</code>. Used only for diagnostic messages.
   */
  @Nullable
  default String getName ()
  {
    return null;
  }
}
