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

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Opt-in bootstrap of the OpenTelemetry SDK for the kaltblut CLI. Installation is skipped unless
 * the user explicitly enables it via the system property {@code otel.enabled=true} or the
 * environment variable {@code OTEL_ENABLED=true}. All exporter / endpoint / sampling configuration
 * is applied through the standard OpenTelemetry environment variables (e.g.
 * {@code OTEL_EXPORTER_OTLP_ENDPOINT}, {@code OTEL_SERVICE_NAME}, {@code OTEL_TRACES_EXPORTER}),
 * which the SDK autoconfigure module reads.
 *
 * @author Philip Helger
 */
public final class KaltblutOtelBootstrap
{
  private static final Logger LOGGER = LoggerFactory.getLogger (KaltblutOtelBootstrap.class);

  private KaltblutOtelBootstrap ()
  {}

  private static boolean _isEnabled ()
  {
    if ("true".equalsIgnoreCase (System.getProperty (CKaltblutOtel.PROPERTY_OTEL_ENABLED)))
      return true;

    return "true".equalsIgnoreCase (System.getenv ("OTEL_ENABLED"));
  }

  /**
   * Install the OpenTelemetry SDK as the global instance, if telemetry is enabled. Must run before
   * the first span is created, because the ph-telemetry-otel binding caches the resolved tracer on
   * first use.
   *
   * @return the installed SDK to be shut down on exit, or <code>null</code> if telemetry is
   *         disabled.
   */
  @Nullable
  public static OpenTelemetrySdk initOrNull ()
  {
    if (!_isEnabled ())
    {
      // Telemetry is off. Silence the one-time JUL INFO hint that GlobalOpenTelemetry emits when
      // the autoconfigure module is on the classpath but not enabled - it would otherwise print on
      // the first span of every CLI run.
      java.util.logging.Logger.getLogger ("io.opentelemetry.api.GlobalOpenTelemetry").setLevel (Level.WARNING);
      return null;
    }

    LOGGER.info ("Initializing OpenTelemetry via SDK autoconfigure");
    // setResultAsGlobal=true makes GlobalOpenTelemetry.get() return this instance, so the
    // KaltblutOtelTracerSPI (loaded via ServiceLoader by the Telemetry facade) resolves it.
    final OpenTelemetrySdk aSdk = AutoConfiguredOpenTelemetrySdk.builder ()
                                                                .setResultAsGlobal ()
                                                                .build ()
                                                                .getOpenTelemetrySdk ();
    LOGGER.info ("Successfully installed the OpenTelemetry SDK: " + aSdk.getClass ().getName ());
    return aSdk;
  }

  /**
   * Flush any pending spans and shut the SDK down. No-op if <code>aSdk</code> is <code>null</code>.
   *
   * @param aSdk
   *        the SDK returned by {@link #initOrNull()}. May be <code>null</code>.
   */
  public static void shutdown (@Nullable final OpenTelemetrySdk aSdk)
  {
    if (aSdk != null)
    {
      aSdk.getSdkTracerProvider ().forceFlush ().join (10, TimeUnit.SECONDS);
      aSdk.close ();
      LOGGER.info ("Flushed and shut down the OpenTelemetry SDK");
    }
  }
}
