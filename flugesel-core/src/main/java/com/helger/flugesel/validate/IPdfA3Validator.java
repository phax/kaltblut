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

import java.io.IOException;

import org.jspecify.annotations.NonNull;

import com.helger.collection.commons.ICommonsList;
import com.helger.flugesel.source.IHybridSource;

/**
 * SPI for PDF/A-3 validation.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. The
 * <code>flugesel-verapdf</code> module registers a veraPDF-backed implementation.
 * <code>flugesel-core</code> ships no default implementation; if none is registered the
 * {@link HybridValidator} will record a single {@link ESeverity#INFORMATION} finding
 * stating that PDF/A-3 validation is not configured.
 *
 * @author Philip Helger
 */
public interface IPdfA3Validator
{
  /**
   * Validate that the given source is a conformant PDF/A-3 (or PDF/A-4f) document. The
   * implementation should produce findings against well-known PDF/A-3 rule IDs (e.g.
   * <code>veraPDF</code>'s clause/test-number identifiers) at appropriate severities. The
   * <code>BR-FX-DE-03</code> downgrade for German DE↔DE invoices is applied by
   * {@link HybridValidator}, not here.
   *
   * @param aSource
   *        the source.
   * @return the findings. Empty list = no PDF/A-3 issues found.
   * @throws IOException
   *         on I/O failure.
   */
  @NonNull
  ICommonsList <Finding> validatePdfA3 (@NonNull IHybridSource aSource) throws IOException;
}
