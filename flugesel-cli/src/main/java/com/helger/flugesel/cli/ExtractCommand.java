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
package com.helger.flugesel.cli;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.flugesel.core.extract.HybridExtractor;
import com.helger.flugesel.core.source.HybridSource;
import com.helger.flugesel.core.source.IHybridSource;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command (name = "extract", description = "Extract the embedded invoice XML from each input PDF.")
public final class ExtractCommand implements Callable <Integer>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ExtractCommand.class);

  @Option (names = { "-o", "--output-dir" },
           description = "Directory to write extracted XML files to (default: current directory).")
  private String m_sOutputDir = ".";

  @Option (names = { "-s",
                     "--suffix" },
           description = "Output filename suffix (default: -invoice). Final name = <pdfBaseName><suffix>.xml")
  private String m_sSuffix = "-invoice";

  @Parameters (paramLabel = "PDF", description = "One or more PDF files to extract from", arity = "1..*")
  private List <String> m_aFiles;

  @SuppressWarnings ("unused")
  private void _dummy ()
  {
    m_sOutputDir = null;
    m_sSuffix = null;
    m_aFiles = null;
  }

  @Override
  public Integer call ()
  {
    final File aOutDir = new File (m_sOutputDir).getAbsoluteFile ();
    if (!aOutDir.exists () && !aOutDir.mkdirs ())
    {
      LOGGER.error ("Failed to create output directory '" + aOutDir.getAbsolutePath () + "'");
      return Integer.valueOf (1);
    }

    int nErrors = 0;
    int nSuccess = 0;
    for (final String sPath : m_aFiles)
    {
      final File aFile = new File (sPath).getAbsoluteFile ();
      if (!aFile.isFile () || !aFile.canRead ())
      {
        LOGGER.error ("Cannot read PDF '" + aFile.getAbsolutePath () + "'");
        nErrors++;
        continue;
      }
      try
      {
        final IHybridSource aSource = HybridSource.fromFile (aFile);
        final byte [] aXml = HybridExtractor.extractInvoiceXml (aSource);
        if (aXml == null)
        {
          LOGGER.warn ("No invoice XML found in '" + aFile.getName () + "'");
          nErrors++;
          continue;
        }
        String sBaseName = aFile.getName ();
        final int nDot = sBaseName.lastIndexOf ('.');
        if (nDot > 0)
          sBaseName = sBaseName.substring (0, nDot);
        final File aOutFile = new File (aOutDir, sBaseName + m_sSuffix + ".xml");
        Files.write (aOutFile.toPath (), aXml);
        LOGGER.info ("Extracted " + aXml.length + " bytes to '" + aOutFile.getName () + "'");
        nSuccess++;
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to extract from '" + aFile.getAbsolutePath () + "': " + ex.getMessage ());
        nErrors++;
      }
    }
    LOGGER.info ("Done. " + nSuccess + " file(s) extracted, " + nErrors + " error(s).");
    return Integer.valueOf (nErrors > 0 ? 1 : 0);
  }
}
