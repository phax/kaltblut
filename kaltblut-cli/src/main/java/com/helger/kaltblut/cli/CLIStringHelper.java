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

import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;

/**
 * Helpers for rendering untrusted, PDF-derived strings (embedded-file names, XMP metadata values,
 * finding messages) on the console. Attacker-controlled strings must never be written verbatim: CR
 * / LF let a malicious PDF forge extra output lines, and ANSI / OSC escape sequences let it corrupt
 * or retitle the terminal.
 *
 * @author Philip Helger
 */
@Immutable
final class CLIStringHelper
{
  private CLIStringHelper ()
  {}

  /**
   * Replace every ISO control character (CR, LF, ESC, ...) with '?' so the value is safe to print
   * on a single console line.
   *
   * @param s
   *        the string to sanitize. May be <code>null</code>.
   * @return the sanitized string, or <code>null</code> if the input was <code>null</code>.
   */
  @Nullable
  static String getConsoleSafe (@Nullable final String s)
  {
    if (s == null)
      return null;
    final StringBuilder aSB = new StringBuilder (s.length ());
    for (int i = 0; i < s.length (); i++)
    {
      final char c = s.charAt (i);
      aSB.append (Character.isISOControl (c) ? '?' : c);
    }
    return aSB.toString ();
  }
}
