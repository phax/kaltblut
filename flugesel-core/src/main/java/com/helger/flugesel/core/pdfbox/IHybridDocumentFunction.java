package com.helger.flugesel.core.pdfbox;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.flugesel.core.source.IHybridSource;

/**
 * Functional interface for {@link #withOpenDocument(IHybridSource, IHybridDocumentFunction)}.
 *
 * @param <T>
 *        The response type
 * @author Philip Helger
 */
@FunctionalInterface
public interface IHybridDocumentFunction <T>
{
  @Nullable
  T apply (@NonNull HybridDocument aDoc) throws IOException;
}