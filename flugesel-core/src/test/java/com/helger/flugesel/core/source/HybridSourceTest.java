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
package com.helger.flugesel.core.source;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.junit.Test;

public final class HybridSourceTest
{
  @Test
  public void testFromBytesReturnsSameArray () throws IOException
  {
    final byte [] aData = "%PDF-1.7\nfoo".getBytes ();
    final IHybridSource aSource = HybridSource.fromBytes (aData);
    assertEquals (aData.length, aSource.getSize ());
    // The same backing array is returned each call (no defensive copy by contract).
    assertSame (aData, aSource.getBytes ());
    assertSame (aData, aSource.getBytes ());
  }

  @Test
  public void testFromBytesSliceCopies () throws IOException
  {
    final byte [] aData = "AAAhelloBBB".getBytes ();
    final IHybridSource aSource = HybridSource.fromBytes (aData, 3, 5);
    assertEquals (5L, aSource.getSize ());
    assertArrayEquals ("hello".getBytes (), aSource.getBytes ());
  }

  @Test
  public void testFromByteBufferCopies () throws IOException
  {
    final byte [] aData = "wrapped".getBytes ();
    final ByteBuffer aBuf = ByteBuffer.wrap (aData);
    final IHybridSource aSource = HybridSource.fromByteBuffer (aBuf);
    assertArrayEquals (aData, aSource.getBytes ());
    // The buffer position must be unchanged.
    assertEquals (0, aBuf.position ());
  }

  @Test
  public void testFromByteBufferReadOnly () throws IOException
  {
    final byte [] aData = "readonly".getBytes ();
    final ByteBuffer aBuf = ByteBuffer.wrap (aData).asReadOnlyBuffer ();
    final IHybridSource aSource = HybridSource.fromByteBuffer (aBuf);
    assertArrayEquals (aData, aSource.getBytes ());
  }

  @Test
  public void testFromFileLazyReadAndCaches () throws IOException
  {
    final File aTmp = File.createTempFile ("flugesel-test-", ".bin");
    try
    {
      final byte [] aData = "filebytes".getBytes ();
      Files.write (aTmp.toPath (), aData);
      final IHybridSource aSource = HybridSource.fromFile (aTmp);
      assertEquals (aData.length, aSource.getSize ());
      assertEquals (aTmp.getName (), aSource.getName ());
      assertArrayEquals (aData, aSource.getBytes ());
      // Subsequent call returns the cached array (identity check).
      assertSame (aSource.getBytes (), aSource.getBytes ());
    }
    finally
    {
      aTmp.delete ();
    }
  }

  @Test
  public void testFromInputStream () throws IOException
  {
    final byte [] aData = "stream".getBytes ();
    final IHybridSource aSource = HybridSource.fromInputStream (new ByteArrayInputStream (aData));
    assertArrayEquals (aData, aSource.getBytes ());
    // Calling getBytes() again returns the same materialised array.
    assertSame (aSource.getBytes (), aSource.getBytes ());
  }

  @Test
  public void testFromClasspath () throws IOException
  {
    final IHybridSource aSource = HybridSource.fromClasspath ("external/zugferd/2.0.1/zugferd_2p0_EN16931_Einfach.pdf");
    assertTrue ("PDF resource must have non-zero size", aSource.getSize () > 0);
    final byte [] aBytes = aSource.getBytes ();
    assertTrue ("PDF bytes should start with %PDF", aBytes[0] == (byte) '%' && aBytes[1] == (byte) 'P');
    assertEquals ("external/zugferd/2.0.1/zugferd_2p0_EN16931_Einfach.pdf", aSource.getName ());
  }

  @Test (expected = IOException.class)
  public void testFromClasspathMissingThrows () throws IOException
  {
    HybridSource.fromClasspath ("external/does-not-exist.pdf");
  }
}
