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
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.flugesel.extract.HybridExtractor;
import com.helger.flugesel.model.HybridAttachment;
import com.helger.flugesel.source.HybridSource;
import com.helger.flugesel.source.IHybridSource;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command (name = "attachments", description = "List embedded files (invoice XML + supporting documents) in each input PDF.")
public final class AttachmentsCommand implements Callable <Integer>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AttachmentsCommand.class);

  @Parameters (paramLabel = "PDF", description = "One or more PDF files to inspect", arity = "1..*")
  private List <String> m_aFiles;

  @SuppressWarnings ("unused")
  private void _dummy ()
  {
    m_aFiles = null;
  }

  @Override
  public Integer call ()
  {
    int nErrors = 0;
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
        final List <HybridAttachment> aAtts = HybridExtractor.listAttachments (aSource);
        System.out.println ("File: " + aFile.getName () + " (" + aAtts.size () + " attachment(s))");
        for (final HybridAttachment aAtt : aAtts)
          System.out.println ("  - " +
                              aAtt.getName () +
                              "  [" +
                              (aAtt.getMimeType () == null ? "?" : aAtt.getMimeType ()) +
                              "]  AFRel=" +
                              aAtt.getRawAFRelationship () +
                              "  size=" +
                              aAtt.getSize () +
                              (aAtt.isInvoiceXml () ? "  (invoice XML)" : ""));
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Failed to list attachments for '" + aFile.getAbsolutePath () + "': " + ex.getMessage ());
        nErrors++;
      }
    }
    return Integer.valueOf (nErrors > 0 ? 1 : 0);
  }
}
