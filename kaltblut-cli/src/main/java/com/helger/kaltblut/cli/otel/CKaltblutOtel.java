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
package com.helger.kaltblut.cli.otel;

import com.helger.annotation.concurrent.Immutable;

/**
 * OpenTelemetry-related constants for the kaltblut CLI.
 *
 * @author Philip Helger
 */
@Immutable
public final class CKaltblutOtel
{
  /** The OpenTelemetry instrumentation scope name for all kaltblut spans. */
  public static final String INSTRUMENTATION_SCOPE_NAME = "com.helger.kaltblut";

  /**
   * The OpenTelemetry instrumentation scope version. Kept in sync manually with the parent POM
   * version; it is informational only and does not affect span correlation.
   */
  public static final String INSTRUMENTATION_SCOPE_VERSION = "0.9.3";

  /**
   * System property / environment variable that opts the CLI in to installing the OpenTelemetry SDK.
   * Checked as the system property {@code otel.enabled} and the environment variable
   * {@code OTEL_ENABLED}; either being {@code true} enables the export pipeline.
   */
  public static final String PROPERTY_OTEL_ENABLED = "otel.enabled";

  private CKaltblutOtel ()
  {}
}
