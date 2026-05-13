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

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.kaltblut.core.validate.HybridFinding;
import com.helger.kaltblut.core.validate.HybridValidator;
import com.helger.kaltblut.core.validate.ValidationResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command (name = "validate",
          description = "Run BR-HYBRID-* business rules and (optionally) PDF/A-3 validation against each input PDF. " +
                        "Exit code is 0 iff no FATAL findings are reported.")
public final class ValidateCommand implements Callable <Integer>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ValidateCommand.class);

  @Option (names = { "-c", "--country" },
           description = "Country context for country-specific rules: ${COMPLETION-CANDIDATES} (default: OTHER).")
  private EZugferdCountry m_eCountry = EZugferdCountry.OTHER;

  @Option (names = { "--no-pdfa" }, description = "Skip PDF/A-3 validation via the IPdfA3Validator SPI.")
  private boolean m_bNoPdfA;

  @Option (names = { "--no-de-pdfa-downgrade" },
           description = "Disable BR-FX-DE-03 PDF/A-3 error downgrade for DE↔DE invoices.")
  private boolean m_bNoDeDowngrade;

  @Parameters (paramLabel = "PDF", description = "One or more PDF files to validate", arity = "1..*")
  private List <String> m_aFiles;

  @SuppressWarnings ("unused")
  private void _dummy ()
  {
    m_eCountry = null;
    m_aFiles = null;
  }

  @Override
  public Integer call ()
  {
    int nFatal = 0;
    int nWarn = 0;
    int nInfo = 0;
    int nFatalFiles = 0;
    for (final String sPath : m_aFiles)
    {
      final File aFile = new File (sPath).getAbsoluteFile ();
      if (!aFile.isFile () || !aFile.canRead ())
      {
        LOGGER.error ("Cannot read PDF '" + aFile.getAbsolutePath () + "'");
        nFatalFiles++;
        continue;
      }
      try
      {
        final IHybridSource aSource = HybridSource.fromFile (aFile);
        final HybridValidator aValidator = new HybridValidator ();
        aValidator.getSettings ().setCountry (m_eCountry);
        aValidator.getSettings ().setCheckPdfA3 (!m_bNoPdfA);
        aValidator.getSettings ().setApplyDePdfADowngrade (!m_bNoDeDowngrade);
        final ValidationResult aRes = aValidator.validate (aSource);
        System.out.println ("File: " + aFile.getName () + "  (" + aRes.getFindingCount () + " finding(s))");
        for (final HybridFinding aF : aRes.getAllFindings ())
        {
          System.out.println ("  " + aF);
          switch (aF.getSeverity ())
          {
            case FATAL:
              nFatal++;
              break;
            case WARNING:
              nWarn++;
              break;
            case INFORMATION:
            default:
              nInfo++;
              break;
          }
        }
        if (aRes.hasFatal ())
          nFatalFiles++;
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to validate '" + aFile.getAbsolutePath () + "': " + ex.getMessage ());
        nFatalFiles++;
      }
    }
    LOGGER.info ("Total: " + nFatal + " fatal, " + nWarn + " warning(s), " + nInfo + " info.");
    return Integer.valueOf (nFatalFiles > 0 ? 1 : 0);
  }
}
