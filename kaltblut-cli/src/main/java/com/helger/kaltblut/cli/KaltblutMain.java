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
package com.helger.kaltblut.cli;

import com.helger.kaltblut.cli.otel.KaltblutOtelBootstrap;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Command-line front-end for kaltblut. Wraps the four library entry points as picocli
 * subcommands.
 *
 * @author Philip Helger
 */
@Command (name = "kaltblut",
          mixinStandardHelpOptions = true,
          description = "ZUGFeRD / Factur-X hybrid invoice toolkit (detect, extract, validate)",
          subcommands = { InspectCommand.class,
                          ExtractCommand.class,
                          AttachmentsCommand.class,
                          ValidateCommand.class })
public final class KaltblutMain
{
  public static void main (final String [] aArgs)
  {
    // Install the OpenTelemetry SDK (opt-in) before any span is created; null when disabled.
    final OpenTelemetrySdk aSdk = KaltblutOtelBootstrap.initOrNull ();
    int nExitCode;
    try
    {
      final CommandLine cmd = new CommandLine (new KaltblutMain ());
      cmd.setCaseInsensitiveEnumValuesAllowed (true);
      nExitCode = cmd.execute (aArgs);
    }
    finally
    {
      KaltblutOtelBootstrap.shutdown (aSdk);
    }
    System.exit (nExitCode);
  }
}
