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
package com.helger.kaltblut.verapdf;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.TestAssertion.Status;
import org.verapdf.pdfa.results.ValidationResult;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.kaltblut.core.validate.EHybridSeverity;
import com.helger.kaltblut.core.validate.HybridFinding;
import com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI;

/**
 * veraPDF-backed implementation of {@link IPdfA3ValidatorSPI}. Registered via
 * <code>META-INF/services/com.helger.kaltblut.core.validate.IPdfA3ValidatorSPI</code>.
 * <p>
 * The validator auto-detects the PDF/A flavor from the document itself and only proceeds if it is
 * PDF/A-3 (3a / 3b / 3u) or PDF/A-4f. Other flavors produce a single {@code FATAL} finding.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class VeraPdfA3ValidatorSPI implements IPdfA3ValidatorSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (VeraPdfA3ValidatorSPI.class);
  private static volatile boolean s_bInitialised = false;

  public VeraPdfA3ValidatorSPI ()
  {
    // ServiceLoader requires a public no-arg constructor.
  }

  private static synchronized void _ensureInitialised ()
  {
    if (!s_bInitialised)
    {
      VeraGreenfieldFoundryProvider.initialise ();
      s_bInitialised = true;
    }
  }

  private static boolean _isAcceptable (@NonNull final PDFAFlavour eF)
  {
    if (eF == null || eF == PDFAFlavour.NO_FLAVOUR)
      return false;
    final String sID = eF.getId ();
    return sID != null && (sID.startsWith ("3") || "4f".equals (sID));
  }

  @Override
  @NonNull
  public ICommonsList <HybridFinding> validatePdfA3 (@NonNull final IHybridSource aSource) throws IOException
  {
    _ensureInitialised ();

    final ICommonsList <HybridFinding> aOut = new CommonsArrayList <> ();
    try (final InputStream aIS = new NonBlockingByteArrayInputStream (aSource.getBytes ()))
    {
      try (final PDFAParser aParser = Foundries.defaultInstance ().createParser (aIS, PDFAFlavour.NO_FLAVOUR))
      {
        final PDFAFlavour eDetected = aParser.getFlavour ();
        if (!_isAcceptable (eDetected))
        {
          aOut.add (new HybridFinding ("VERAPDF-FLAVOUR",
                                       EHybridSeverity.FATAL,
                                       "PDF/A flavor '" +
                                                              eDetected +
                                                              "' is not acceptable for a hybrid invoice. " +
                                                              "Expected PDF/A-3 (3a, 3b or 3u) or PDF/A-4f.",
                                       null));
          return aOut;
        }

        try (final PDFAValidator aValidator = Foundries.defaultInstance ().createValidator (eDetected, false))
        {
          final ValidationResult aRes = aValidator.validate (aParser);
          for (final TestAssertion aTA : aRes.getTestAssertions ())
            if (aTA.getStatus () == Status.FAILED)
              aOut.add (new HybridFinding ("VERAPDF-" +
                                           aTA.getRuleId ().getClause () +
                                           "-" +
                                           aTA.getRuleId ().getTestNumber (),
                                           EHybridSeverity.FATAL,
                                           aTA.getMessage (),
                                           aTA.getLocation () != null ? aTA.getLocation ().getContext () : null));

          if (aOut.isEmpty () && !aRes.isCompliant ())
            aOut.add (new HybridFinding ("VERAPDF",
                                         EHybridSeverity.FATAL,
                                         "PDF/A validation reported non-compliance but produced no detailed assertions.",
                                         null));
        }
      }
    }
    catch (final IOException ex)
    {
      throw ex;
    }
    catch (final Exception ex)
    {
      LOGGER.warn ("veraPDF validation failed", ex);
      aOut.add (new HybridFinding ("VERAPDF-ERROR",
                                   EHybridSeverity.WARNING,
                                   "veraPDF validation failed: " + ex.getMessage (),
                                   null));
    }
    return aOut;
  }
}
