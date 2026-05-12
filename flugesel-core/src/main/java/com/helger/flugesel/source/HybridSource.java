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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;

/**
 * Factory methods for {@link IHybridSource} instances.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridSource
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HybridSource.class);

  private HybridSource ()
  {}

  /**
   * Create a re-readable source backed by a byte array. The array is referenced, not copied;
   * callers must not mutate it.
   *
   * @param aBytes
   *        the bytes of the PDF. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromBytes (@NonNull final byte [] aBytes)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    return fromBytes (aBytes, 0, aBytes.length);
  }

  /**
   * Create a re-readable source backed by a slice of a byte array. The array is referenced, not
   * copied; callers must not mutate it.
   *
   * @param aBytes
   *        the bytes of the PDF. May not be <code>null</code>.
   * @param nOfs
   *        offset into <code>aBytes</code>.
   * @param nLen
   *        number of bytes to read.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromBytes (@NonNull final byte [] aBytes, final int nOfs, final int nLen)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    ValueEnforcer.isArrayOfsLen (aBytes, nOfs, nLen);
    return new ByteArrayHybridSource (aBytes, nOfs, nLen, null);
  }

  /**
   * Create a re-readable source backed by a {@link ByteBuffer}. The buffer contents from
   * <code>position()</code> to <code>limit()</code> at the time of this call are captured by
   * reference into a byte array if the buffer is non-direct and has an accessible array;
   * otherwise the contents are copied.
   *
   * @param aBuffer
   *        the byte buffer. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromByteBuffer (@NonNull final ByteBuffer aBuffer)
  {
    ValueEnforcer.notNull (aBuffer, "Buffer");
    final int nRemaining = aBuffer.remaining ();
    if (aBuffer.hasArray () && !aBuffer.isReadOnly ())
    {
      final byte [] aArr = aBuffer.array ();
      final int nOfs = aBuffer.arrayOffset () + aBuffer.position ();
      return new ByteArrayHybridSource (aArr, nOfs, nRemaining, null);
    }
    final byte [] aCopy = new byte [nRemaining];
    final int nPos = aBuffer.position ();
    aBuffer.get (aCopy);
    aBuffer.position (nPos);
    return new ByteArrayHybridSource (aCopy, 0, nRemaining, null);
  }

  /**
   * Create a re-readable source backed by a file.
   *
   * @param aFile
   *        the file. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromFile (@NonNull final File aFile)
  {
    ValueEnforcer.notNull (aFile, "File");
    return new FileHybridSource (aFile.toPath ());
  }

  /**
   * Create a re-readable source backed by a path.
   *
   * @param aPath
   *        the path. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromPath (@NonNull final Path aPath)
  {
    ValueEnforcer.notNull (aPath, "Path");
    return new FileHybridSource (aPath);
  }

  /**
   * Create a re-readable source backed by a URL. The URL is re-opened on each input stream
   * acquisition; callers should prefer {@link #fromBytes} or {@link #fromFile} for repeated
   * access if the resource is large.
   *
   * @param aUrl
   *        the URL. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   */
  @NonNull
  public static IHybridSource fromUrl (@NonNull final URL aUrl)
  {
    ValueEnforcer.notNull (aUrl, "URL");
    return new UrlHybridSource (aUrl);
  }

  /**
   * Create a single-read source from an {@link InputStream}. The stream is consumed on the first
   * call to {@link IHybridSource#getInputStream()}; subsequent calls return <code>null</code>.
   * The caller is responsible for closing the returned stream, but the stream returned will be
   * the original stream.
   *
   * @param aIS
   *        the input stream. May not be <code>null</code>.
   * @return a single-read hybrid source.
   */
  @NonNull
  public static IHybridSource fromInputStreamOnce (@NonNull final InputStream aIS)
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    return new SingleReadInputStreamHybridSource (aIS);
  }

  /**
   * Create a re-readable source from a classpath resource. The resource is resolved via the given
   * class's {@link Class#getResourceAsStream(String)}, which honours classloader scoping. The
   * stream contents are materialised eagerly to memory.
   *
   * @param sResourcePath
   *        the resource path, absolute (starting with {@code /}) or relative to the loader class.
   * @param aLoader
   *        the class whose classloader is used. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   * @throws IOException
   *         if the resource is not found or cannot be read.
   */
  @NonNull
  public static IHybridSource fromClasspath (@NonNull final String sResourcePath,
                                             @NonNull final Class <?> aLoader) throws IOException
  {
    ValueEnforcer.notNull (sResourcePath, "ResourcePath");
    ValueEnforcer.notNull (aLoader, "Loader");
    try (final InputStream aIS = aLoader.getResourceAsStream (sResourcePath))
    {
      if (aIS == null)
        throw new IOException ("Classpath resource not found: " + sResourcePath);
      final byte [] aBytes = StreamHelper.getAllBytes (aIS);
      if (aBytes == null)
        throw new IOException ("Failed to read classpath resource: " + sResourcePath);
      return new ByteArrayHybridSource (aBytes, 0, aBytes.length, sResourcePath);
    }
  }

  /**
   * Create a re-readable source from a classpath resource resolved via the current thread's
   * context classloader (falling back to the class loader of this class).
   *
   * @param sResourcePath
   *        the resource path (must not start with {@code /}).
   * @return a re-readable hybrid source.
   * @throws IOException
   *         if the resource is not found or cannot be read.
   */
  @NonNull
  public static IHybridSource fromClasspath (@NonNull final String sResourcePath) throws IOException
  {
    ValueEnforcer.notNull (sResourcePath, "ResourcePath");
    ClassLoader aCL = Thread.currentThread ().getContextClassLoader ();
    if (aCL == null)
      aCL = HybridSource.class.getClassLoader ();
    try (final InputStream aIS = aCL.getResourceAsStream (sResourcePath))
    {
      if (aIS == null)
        throw new IOException ("Classpath resource not found: " + sResourcePath);
      final byte [] aBytes = StreamHelper.getAllBytes (aIS);
      if (aBytes == null)
        throw new IOException ("Failed to read classpath resource: " + sResourcePath);
      return new ByteArrayHybridSource (aBytes, 0, aBytes.length, sResourcePath);
    }
  }

  /**
   * Materialize an {@link InputStream} into a re-readable in-memory byte array source. The input
   * stream is closed after reading.
   *
   * @param aIS
   *        the input stream. May not be <code>null</code>.
   * @return a re-readable hybrid source.
   * @throws IOException
   *         if reading from the stream failed.
   */
  @NonNull
  public static IHybridSource materialize (@NonNull final InputStream aIS) throws IOException
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    final byte [] aBytes = StreamHelper.getAllBytes (aIS);
    if (aBytes == null)
      throw new IOException ("Failed to read input stream into byte array");
    return new ByteArrayHybridSource (aBytes, 0, aBytes.length, null);
  }

  // ----- internal implementations -----

  private static final class ByteArrayHybridSource implements IHybridSource
  {
    private final byte [] m_aBytes;
    private final int m_nOfs;
    private final int m_nLen;
    private final String m_sName;

    ByteArrayHybridSource (final byte [] aBytes, final int nOfs, final int nLen, @Nullable final String sName)
    {
      m_aBytes = aBytes;
      m_nOfs = nOfs;
      m_nLen = nLen;
      m_sName = sName;
    }

    @Override
    public InputStream getInputStream ()
    {
      return new NonBlockingByteArrayInputStream (m_aBytes, m_nOfs, m_nLen);
    }

    @Override
    public boolean isReadMultiple ()
    {
      return true;
    }

    @Override
    public long getSize ()
    {
      return m_nLen;
    }

    @Override
    @Nullable
    public String getName ()
    {
      return m_sName;
    }
  }

  private static final class FileHybridSource implements IHybridSource
  {
    private final Path m_aPath;

    FileHybridSource (final Path aPath)
    {
      m_aPath = aPath;
    }

    @Override
    @Nullable
    public InputStream getInputStream ()
    {
      try
      {
        return Files.newInputStream (m_aPath);
      }
      catch (final IOException ex)
      {
        LOGGER.warn ("Failed to open file '" + m_aPath + "' for reading", ex);
        return null;
      }
    }

    @Override
    public boolean isReadMultiple ()
    {
      return true;
    }

    @Override
    public long getSize ()
    {
      try
      {
        return Files.size (m_aPath);
      }
      catch (final IOException ex)
      {
        return -1L;
      }
    }

    @Override
    @Nullable
    public String getName ()
    {
      final Path aFile = m_aPath.getFileName ();
      return aFile == null ? m_aPath.toString () : aFile.toString ();
    }

    @Nullable
    Path getPath ()
    {
      return m_aPath;
    }
  }

  private static final class UrlHybridSource implements IHybridSource
  {
    private final URL m_aUrl;

    UrlHybridSource (final URL aUrl)
    {
      m_aUrl = aUrl;
    }

    @Override
    @Nullable
    public InputStream getInputStream ()
    {
      try
      {
        return m_aUrl.openStream ();
      }
      catch (final IOException ex)
      {
        LOGGER.warn ("Failed to open URL '" + m_aUrl + "' for reading", ex);
        return null;
      }
    }

    @Override
    public boolean isReadMultiple ()
    {
      return true;
    }

    @Override
    @Nullable
    public String getName ()
    {
      return m_aUrl.toString ();
    }
  }

  private static final class SingleReadInputStreamHybridSource implements IHybridSource
  {
    private final InputStream m_aIS;
    private final AtomicBoolean m_aConsumed = new AtomicBoolean (false);

    SingleReadInputStreamHybridSource (final InputStream aIS)
    {
      m_aIS = aIS;
    }

    @Override
    @Nullable
    public InputStream getInputStream ()
    {
      if (m_aConsumed.compareAndSet (false, true))
        return m_aIS;
      LOGGER.warn ("Single-read hybrid source has already been consumed");
      return null;
    }

    @Override
    public boolean isReadMultiple ()
    {
      return false;
    }
  }

  /**
   * If the supplied source is re-readable, return it as-is. Otherwise, drain it to memory and
   * return a re-readable copy.
   *
   * @param aSource
   *        the source. May not be <code>null</code>.
   * @return a re-readable equivalent of <code>aSource</code>.
   * @throws IOException
   *         if draining a single-read source failed.
   */
  @NonNull
  public static IHybridSource ensureReadMultiple (@NonNull final IHybridSource aSource) throws IOException
  {
    ValueEnforcer.notNull (aSource, "Source");
    if (aSource.isReadMultiple ())
      return aSource;
    try (final InputStream aIS = aSource.getInputStream ())
    {
      if (aIS == null)
        throw new IOException ("Source already consumed");
      return materialize (aIS);
    }
  }
}
