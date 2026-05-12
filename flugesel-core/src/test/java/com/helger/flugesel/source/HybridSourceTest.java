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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.junit.Test;

import com.helger.base.io.stream.StreamHelper;

public final class HybridSourceTest
{
  private static byte [] _readAll (final IHybridSource aSource) throws IOException
  {
    try (final InputStream aIS = aSource.getInputStream ())
    {
      assertNotNull (aIS);
      return StreamHelper.getAllBytes (aIS);
    }
  }

  @Test
  public void testFromBytesIsReadMultiple () throws IOException
  {
    final byte [] aData = "%PDF-1.7\nfoo".getBytes ();
    final IHybridSource aSource = HybridSource.fromBytes (aData);
    assertTrue (aSource.isReadMultiple ());
    assertEquals (aData.length, aSource.getSize ());
    assertArrayEquals (aData, _readAll (aSource));
    // Second read works.
    assertArrayEquals (aData, _readAll (aSource));
  }

  @Test
  public void testFromBytesSlice () throws IOException
  {
    final byte [] aData = "AAAhelloBBB".getBytes ();
    final IHybridSource aSource = HybridSource.fromBytes (aData, 3, 5);
    assertEquals (5L, aSource.getSize ());
    assertArrayEquals ("hello".getBytes (), _readAll (aSource));
  }

  @Test
  public void testFromByteBuffer () throws IOException
  {
    final byte [] aData = "wrapped".getBytes ();
    final ByteBuffer aBuf = ByteBuffer.wrap (aData);
    final IHybridSource aSource = HybridSource.fromByteBuffer (aBuf);
    assertTrue (aSource.isReadMultiple ());
    assertArrayEquals (aData, _readAll (aSource));
  }

  @Test
  public void testFromByteBufferCopiesWhenReadOnly () throws IOException
  {
    final byte [] aData = "readonly".getBytes ();
    final ByteBuffer aBuf = ByteBuffer.wrap (aData).asReadOnlyBuffer ();
    final IHybridSource aSource = HybridSource.fromByteBuffer (aBuf);
    assertArrayEquals (aData, _readAll (aSource));
  }

  @Test
  public void testFromFile () throws IOException
  {
    final File aTmp = File.createTempFile ("flugesel-test-", ".bin");
    try
    {
      final byte [] aData = "filebytes".getBytes ();
      Files.write (aTmp.toPath (), aData);
      final IHybridSource aSource = HybridSource.fromFile (aTmp);
      assertTrue (aSource.isReadMultiple ());
      assertEquals (aData.length, aSource.getSize ());
      assertEquals (aTmp.getName (), aSource.getName ());
      assertArrayEquals (aData, _readAll (aSource));
    }
    finally
    {
      aTmp.delete ();
    }
  }

  @Test
  public void testFromInputStreamOnceIsSingleRead () throws IOException
  {
    final byte [] aData = "once".getBytes ();
    final IHybridSource aSource = HybridSource.fromInputStreamOnce (new ByteArrayInputStream (aData));
    assertFalse (aSource.isReadMultiple ());
    assertArrayEquals (aData, _readAll (aSource));
    // Second acquisition returns null (source is exhausted).
    try (final InputStream aIS = aSource.getInputStream ())
    {
      assertNull (aIS);
    }
  }

  @Test
  public void testMaterializeUpgradesToReadMultiple () throws IOException
  {
    final byte [] aData = "buffered".getBytes ();
    final IHybridSource aSource = HybridSource.materialize (new ByteArrayInputStream (aData));
    assertTrue (aSource.isReadMultiple ());
    assertArrayEquals (aData, _readAll (aSource));
    assertArrayEquals (aData, _readAll (aSource));
  }

  @Test
  public void testEnsureReadMultipleIsNoOpForReadable () throws IOException
  {
    final IHybridSource aIn = HybridSource.fromBytes (new byte [] { 1, 2, 3 });
    final IHybridSource aOut = HybridSource.ensureReadMultiple (aIn);
    assertEquals (aIn, aOut);
  }

  @Test
  public void testEnsureReadMultipleUpgradesSingleRead () throws IOException
  {
    final byte [] aData = "upgrade".getBytes ();
    final IHybridSource aIn = HybridSource.fromInputStreamOnce (new ByteArrayInputStream (aData));
    assertFalse (aIn.isReadMultiple ());
    final IHybridSource aOut = HybridSource.ensureReadMultiple (aIn);
    assertTrue (aOut.isReadMultiple ());
    assertArrayEquals (aData, _readAll (aOut));
    assertArrayEquals (aData, _readAll (aOut));
  }

  @Test
  public void testFromClasspath () throws IOException
  {
    // Use one of the shipped sample PDFs from flugesel-testfiles. We do not check the exact bytes
    // (the resource is potentially several MB) — only that the factory produces a re-readable
    // source with a sensible size and name.
    final IHybridSource aSource = HybridSource.fromClasspath ("external/zugferd/2.0.1/zugferd_2p0_EN16931_Einfach.pdf");
    assertTrue (aSource.isReadMultiple ());
    assertTrue ("PDF resource must have non-zero size", aSource.getSize () > 0);
    assertTrue ("PDF bytes should start with %PDF", _readAll (aSource)[0] == (byte) '%');
  }

  @Test (expected = IOException.class)
  public void testFromClasspathMissingThrows () throws IOException
  {
    HybridSource.fromClasspath ("external/does-not-exist.pdf");
  }
}
