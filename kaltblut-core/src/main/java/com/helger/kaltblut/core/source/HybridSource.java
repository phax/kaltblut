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
package com.helger.kaltblut.core.source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;

/**
 * Factory methods for {@link IHybridSource} instances.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridSource
{
  private HybridSource ()
  {}

  /**
   * Create a source backed by a byte array. The array is referenced, not copied; callers must not
   * mutate it.
   *
   * @param aBytes
   *        the bytes of the PDF. May not be <code>null</code>.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromBytes (@NonNull final byte [] aBytes)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    return new EagerBytesHybridSource (aBytes, null);
  }

  /**
   * Create a source backed by a slice of a byte array. The slice is copied into a fresh array so
   * that subsequent mutation of the source array is not observed by this source.
   *
   * @param aBytes
   *        the bytes of the PDF. May not be <code>null</code>.
   * @param nOfs
   *        offset into <code>aBytes</code>.
   * @param nLen
   *        number of bytes to read.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromBytes (@NonNull final byte [] aBytes, final int nOfs, final int nLen)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    ValueEnforcer.isArrayOfsLen (aBytes, nOfs, nLen);
    final byte [] aCopy = new byte [nLen];
    System.arraycopy (aBytes, nOfs, aCopy, 0, nLen);
    return new EagerBytesHybridSource (aCopy, null);
  }

  /**
   * Create a source backed by a {@link ByteBuffer}. The buffer's content from
   * <code>position()</code> to <code>limit()</code> at the time of this call is captured into a
   * fresh array.
   *
   * @param aBuffer
   *        the byte buffer. May not be <code>null</code>.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromByteBuffer (@NonNull final ByteBuffer aBuffer)
  {
    ValueEnforcer.notNull (aBuffer, "Buffer");
    final byte [] aCopy = new byte [aBuffer.remaining ()];
    final int nPos = aBuffer.position ();
    aBuffer.get (aCopy);
    aBuffer.position (nPos);
    return new EagerBytesHybridSource (aCopy, null);
  }

  /**
   * Create a source backed by a file. Bytes are read lazily on the first call to
   * {@link IHybridSource#getBytes()} and cached afterwards.
   *
   * @param aFile
   *        the file. May not be <code>null</code>.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromFile (@NonNull final File aFile)
  {
    ValueEnforcer.notNull (aFile, "File");
    return new PathHybridSource (aFile.toPath ());
  }

  /**
   * Create a source backed by a path. Bytes are read lazily on the first call to
   * {@link IHybridSource#getBytes()} and cached afterwards.
   *
   * @param aPath
   *        the path. May not be <code>null</code>.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromPath (@NonNull final Path aPath)
  {
    ValueEnforcer.notNull (aPath, "Path");
    return new PathHybridSource (aPath);
  }

  /**
   * Create a source backed by a URL. Bytes are read lazily on the first call to
   * {@link IHybridSource#getBytes()} and cached afterwards.
   *
   * @param aUrl
   *        the URL. May not be <code>null</code>.
   * @return a hybrid source.
   */
  @NonNull
  public static IHybridSource fromUrl (@NonNull final URL aUrl)
  {
    ValueEnforcer.notNull (aUrl, "URL");
    return new UrlHybridSource (aUrl);
  }

  /**
   * Read an {@link InputStream} fully and create an in-memory hybrid source. The stream is closed
   * after reading.
   *
   * @param aIS
   *        the input stream. May not be <code>null</code>.
   * @return a hybrid source.
   * @throws IOException
   *         if reading from the stream failed.
   */
  @NonNull
  public static IHybridSource fromInputStream (@NonNull final InputStream aIS) throws IOException
  {
    ValueEnforcer.notNull (aIS, "InputStream");
    final byte [] aBytes = StreamHelper.getAllBytes (aIS);
    if (aBytes == null)
      throw new IOException ("Failed to read input stream into byte array");
    return new EagerBytesHybridSource (aBytes, null);
  }

  /**
   * Create a source from a classpath resource resolved via the given class's classloader. Bytes
   * are read eagerly.
   *
   * @param sResourcePath
   *        the resource path. Absolute (starting with {@code /}) or relative to the loader class.
   * @param aLoader
   *        the class whose classloader is used. May not be <code>null</code>.
   * @return a hybrid source.
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
      return new EagerBytesHybridSource (aBytes, sResourcePath);
    }
  }

  /**
   * Create a source from a classpath resource resolved via the current thread's context
   * classloader (falling back to the class loader of this class). Bytes are read eagerly.
   *
   * @param sResourcePath
   *        the resource path (must not start with {@code /}).
   * @return a hybrid source.
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
      return new EagerBytesHybridSource (aBytes, sResourcePath);
    }
  }

  // ----- internal implementations -----

  /** In-memory eager source. */
  private static final class EagerBytesHybridSource implements IHybridSource
  {
    private final byte [] m_aBytes;
    private final String m_sName;

    EagerBytesHybridSource (@NonNull final byte [] aBytes, @Nullable final String sName)
    {
      m_aBytes = aBytes;
      m_sName = sName;
    }

    @Override
    @NonNull
    public byte [] getBytes ()
    {
      return m_aBytes;
    }

    @Override
    public long getSize ()
    {
      return m_aBytes.length;
    }

    @Override
    @Nullable
    public String getName ()
    {
      return m_sName;
    }
  }

  /** Lazy file/path source with one-shot caching. */
  private static final class PathHybridSource implements IHybridSource
  {
    private final Path m_aPath;
    private byte [] m_aCached;

    PathHybridSource (@NonNull final Path aPath)
    {
      m_aPath = aPath;
    }

    @Override
    @NonNull
    public synchronized byte [] getBytes () throws IOException
    {
      if (m_aCached == null)
        m_aCached = Files.readAllBytes (m_aPath);
      return m_aCached;
    }

    @Override
    public long getSize ()
    {
      if (m_aCached != null)
        return m_aCached.length;
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
  }

  /** Lazy URL source with one-shot caching. */
  private static final class UrlHybridSource implements IHybridSource
  {
    private final URL m_aUrl;
    private byte [] m_aCached;

    UrlHybridSource (@NonNull final URL aUrl)
    {
      m_aUrl = aUrl;
    }

    @Override
    @NonNull
    public synchronized byte [] getBytes () throws IOException
    {
      if (m_aCached == null)
      {
        try (final InputStream aIS = m_aUrl.openStream ())
        {
          final byte [] aBytes = StreamHelper.getAllBytes (aIS);
          if (aBytes == null)
            throw new IOException ("Failed to read URL " + m_aUrl);
          m_aCached = aBytes;
        }
      }
      return m_aCached;
    }

    @Override
    @Nullable
    public String getName ()
    {
      return m_aUrl.toString ();
    }
  }
}
