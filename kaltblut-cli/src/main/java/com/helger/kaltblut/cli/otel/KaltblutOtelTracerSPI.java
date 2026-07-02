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

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.telemetry.otel.OtelTelemetryTracerSPI;

/**
 * Concrete {@link OtelTelemetryTracerSPI} that wires the kaltblut instrumentation scope into the
 * generic ph-telemetry-otel binding. Registered via
 * {@code META-INF/services/com.helger.telemetry.ITelemetryTracerSPI}, so the {@code Telemetry}
 * facade resolves it via {@code ServiceLoader}. It resolves the actual SDK lazily from
 * {@code GlobalOpenTelemetry} on first use; until an SDK is installed it stays a cheap no-op.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class KaltblutOtelTracerSPI extends OtelTelemetryTracerSPI
{
  public KaltblutOtelTracerSPI ()
  {
    super (CKaltblutOtel.INSTRUMENTATION_SCOPE_NAME, CKaltblutOtel.INSTRUMENTATION_SCOPE_VERSION);
  }
}
