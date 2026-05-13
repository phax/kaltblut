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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.ELockType;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.MustBeLocked;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;

/**
 * Factory methods for {@link IHybridSource} instances.
 *
 * @author Philip Helger
 */
@Immutable
public final class HybridSource
{
  /** Default connect timeout for HTTP/HTTPS sources. */
  @NonNull
  public static final Duration DEFAULT_URL_CONNECT_TIMEOUT = Duration.ofSeconds (10);

  /** Default read timeout for HTTP/HTTPS sources. */
  @NonNull
  public static final Duration DEFAULT_URL_READ_TIMEOUT = Duration.ofSeconds (60);

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

    if (nOfs == 0 && nLen == aBytes.length)
    {
      // No need to copy
      return new EagerBytesHybridSource (aBytes, null);
    }

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
   * Create a source backed by an HTTP or HTTPS URL. Bytes are read lazily on the first call to
   * {@link IHybridSource#getBytes()} and cached afterwards.
   * <p>
   * Only the <code>http</code> and <code>https</code> schemes are accepted; other schemes
   * (<code>file:</code>, <code>jar:</code>, <code>ftp:</code>, ...) are rejected to prevent
   * accidental SSRF / local-file-read when a caller forwards untrusted URLs. Connect and read
   * timeouts default to {@link #DEFAULT_URL_CONNECT_TIMEOUT} and {@link #DEFAULT_URL_READ_TIMEOUT}
   * respectively; use {@link #fromUrl(URL, Duration, Duration)} to override.
   *
   * @param aUrl
   *        the URL. May not be <code>null</code>; scheme must be <code>http</code> or
   *        <code>https</code>.
   * @return a hybrid source.
   * @throws IllegalArgumentException
   *         if the URL scheme is not <code>http</code> or <code>https</code>.
   */
  @NonNull
  public static IHybridSource fromUrl (@NonNull final URL aUrl)
  {
    return fromUrl (aUrl, DEFAULT_URL_CONNECT_TIMEOUT, DEFAULT_URL_READ_TIMEOUT);
  }

  /**
   * Create a source backed by an HTTP or HTTPS URL with explicit connect / read timeouts.
   *
   * @param aUrl
   *        the URL. May not be <code>null</code>; scheme must be <code>http</code> or
   *        <code>https</code>.
   * @param aConnectTimeout
   *        connect timeout. May not be <code>null</code> and must be non-negative;
   *        {@link Duration#ZERO} means "infinite" (matches {@link java.net.URLConnection}).
   * @param aReadTimeout
   *        read timeout. May not be <code>null</code> and must be non-negative;
   *        {@link Duration#ZERO} means "infinite".
   * @return a hybrid source.
   * @throws IllegalArgumentException
   *         if the URL scheme is not <code>http</code> or <code>https</code>, or a timeout is
   *         negative.
   */
  @NonNull
  public static IHybridSource fromUrl (@NonNull final URL aUrl,
                                       @NonNull final Duration aConnectTimeout,
                                       @NonNull final Duration aReadTimeout)
  {
    ValueEnforcer.notNull (aUrl, "URL");
    ValueEnforcer.notNull (aConnectTimeout, "ConnectTimeout");
    ValueEnforcer.notNull (aReadTimeout, "ReadTimeout");

    if (aConnectTimeout.isNegative ())
      throw new IllegalArgumentException ("ConnectTimeout must not be negative");

    if (aReadTimeout.isNegative ())
      throw new IllegalArgumentException ("ReadTimeout must not be negative");

    final String sProtocol = aUrl.getProtocol ();
    if (!"http".equalsIgnoreCase (sProtocol) && !"https".equalsIgnoreCase (sProtocol))
      throw new IllegalArgumentException ("URL scheme '" +
                                          sProtocol +
                                          "' is not allowed; only http and https are supported");

    return new UrlHybridSource (aUrl, aConnectTimeout, aReadTimeout);
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
   * Create a source from a classpath resource resolved via the given class's classloader. Bytes are
   * read eagerly.
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
  public static IHybridSource fromClasspath (@NonNull final String sResourcePath, @NonNull final ClassLoader aLoader)
                                                                                                                      throws IOException
  {
    ValueEnforcer.notNull (sResourcePath, "ResourcePath");
    ValueEnforcer.notNull (aLoader, "Loader");

    try (final InputStream aIS = aLoader.getResourceAsStream (sResourcePath))
    {
      if (aIS == null)
        throw new IOException ("Classpath resource '" + sResourcePath + "' not found");

      final byte [] aBytes = StreamHelper.getAllBytes (aIS);
      if (aBytes == null)
        throw new IOException ("Failed to read classpath resource '" + sResourcePath + "'");
      return new EagerBytesHybridSource (aBytes, sResourcePath);
    }
  }

  /**
   * Create a source from a classpath resource resolved via the current thread's context classloader
   * (falling back to the class loader of this class). Bytes are read eagerly.
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

    return fromClasspath (sResourcePath, aCL);
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
    private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
    private final Path m_aPath;
    private byte [] m_aCached;

    PathHybridSource (@NonNull final Path aPath)
    {
      m_aPath = aPath;
    }

    @Override
    @NonNull
    public byte [] getBytes () throws IOException
    {
      // Fast path: cached, read lock only.
      final byte [] aHit = m_aRWLock.readLockedGet ( () -> m_aCached);
      if (aHit != null)
        return aHit;

      // Slow path: acquire write lock and double-check.
      return m_aRWLock.writeLockedGetThrowing ( () -> {
        if (m_aCached == null)
          m_aCached = Files.readAllBytes (m_aPath);
        return m_aCached;
      });
    }

    @Override
    public long getSize ()
    {
      final byte [] aHit = m_aRWLock.readLockedGet ( () -> m_aCached);
      if (aHit != null)
        return aHit.length;

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

  /**
   * Lazy URL source with one-shot caching, http/https only, with timeouts and optional bounded
   * read.
   */
  private static final class UrlHybridSource implements IHybridSource
  {
    private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
    private final URL m_aUrl;
    private final Duration m_aConnectTimeout;
    private final Duration m_aReadTimeout;
    private byte [] m_aCached;

    UrlHybridSource (@NonNull final URL aUrl,
                     @NonNull final Duration aConnectTimeout,
                     @NonNull final Duration aReadTimeout)
    {
      m_aUrl = aUrl;
      m_aConnectTimeout = aConnectTimeout;
      m_aReadTimeout = aReadTimeout;
    }

    /**
     * URLConnection timeouts are expressed as int millis. {@link Duration#toMillis()} would
     * silently overflow for very large durations, so we saturate at {@link Integer#MAX_VALUE}.
     */
    private static int _toMillis (@NonNull final Duration aDuration)
    {
      final long nMillis = aDuration.toMillis ();
      if (nMillis < 0)
        return 0;
      return nMillis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) nMillis;
    }

    @NonNull
    private URLConnection _openConnection () throws IOException
    {
      final URLConnection aConn = m_aUrl.openConnection ();
      aConn.setConnectTimeout (_toMillis (m_aConnectTimeout));
      aConn.setReadTimeout (_toMillis (m_aReadTimeout));
      if (aConn instanceof final HttpURLConnection aHttp)
      {
        // Java's HttpURLConnection follows http<->http and (since 11) refuses cross-protocol
        // redirects between http and https. We rely on that default; do not enable cross-protocol.
        aHttp.setInstanceFollowRedirects (true);
      }
      return aConn;
    }

    /**
     * Fully fetch the URL without limit and cache it. Caller must hold the write lock.
     */
    @NonNull
    @MustBeLocked (ELockType.WRITE)
    private byte [] _fetchUnboundedUnderWriteLock () throws IOException
    {
      final URLConnection aConn = _openConnection ();
      try (final InputStream aIS = aConn.getInputStream ())
      {
        final byte [] aBytes = StreamHelper.getAllBytes (aIS);
        if (aBytes == null)
          throw new IOException ("Failed to read URL " + m_aUrl);
        m_aCached = aBytes;
      }
      return m_aCached;
    }

    /**
     * Fetch the URL but refuse to read more than nMaxBytes. Caller must hold the write lock.
     */
    @NonNull
    @MustBeLocked (ELockType.WRITE)
    private byte [] _fetchBoundedUnderWriteLock (final long nMaxBytes) throws IOException
    {
      final URLConnection aConn = _openConnection ();
      final long nAdvertised = aConn.getContentLengthLong ();
      if (nAdvertised >= 0 && nAdvertised > nMaxBytes)
        throw new IOException ("URL " +
                               m_aUrl +
                               " advertised Content-Length " +
                               nAdvertised +
                               " exceeds limit of " +
                               nMaxBytes +
                               " bytes");
      try (final InputStream aIS = aConn.getInputStream ();
           final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        final byte [] aBuf = new byte [8192];
        long nTotal = 0L;
        int nRead;
        while ((nRead = aIS.read (aBuf)) > 0)
        {
          nTotal += nRead;
          if (nTotal > nMaxBytes)
            throw new IOException ("URL " + m_aUrl + " exceeded limit of " + nMaxBytes + " bytes while reading");
          aBAOS.write (aBuf, 0, nRead);
        }
        m_aCached = aBAOS.toByteArray ();
      }
      return m_aCached;
    }

    @Override
    @NonNull
    public byte [] getBytes () throws IOException
    {
      final byte [] aHit = m_aRWLock.readLockedGet ( () -> m_aCached);
      if (aHit != null)
        return aHit;

      return m_aRWLock.writeLockedGetThrowing ( () -> {
        if (m_aCached != null)
          return m_aCached;
        return _fetchUnboundedUnderWriteLock ();
      });
    }

    @Override
    @NonNull
    public byte [] getBytes (final long nMaxBytes) throws IOException
    {
      if (nMaxBytes < 0)
        return getBytes ();

      final byte [] aHit = m_aRWLock.readLockedGet ( () -> m_aCached);
      if (aHit != null)
      {
        if (aHit.length > nMaxBytes)
          throw new IOException ("URL " +
                                 m_aUrl +
                                 " size " +
                                 aHit.length +
                                 " exceeds limit of " +
                                 nMaxBytes +
                                 " bytes");
        return aHit;
      }

      return m_aRWLock.writeLockedGetThrowing ( () -> {
        if (m_aCached != null)
        {
          if (m_aCached.length > nMaxBytes)
            throw new IOException ("URL " +
                                   m_aUrl +
                                   " size " +
                                   m_aCached.length +
                                   " exceeds limit of " +
                                   nMaxBytes +
                                   " bytes");
          return m_aCached;
        }
        return _fetchBoundedUnderWriteLock (nMaxBytes);
      });
    }

    @Override
    public long getSize ()
    {
      final byte [] aHit = m_aRWLock.readLockedGet ( () -> m_aCached);
      return aHit != null ? aHit.length : -1L;
    }

    @Override
    @Nullable
    public String getName ()
    {
      return m_aUrl.toString ();
    }
  }
}
