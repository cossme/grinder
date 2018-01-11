// Copyright (C) 2007 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util;

/**
 * <p>
 * Provides simple escaping and unescaping for 8-bit strings. The resulting
 * string should survive translation between common line-ending conventions.
 * </p>
 *
 * <p>
 * Escapes ASCII '\', '\n', '\r' by replacing them with literals "\\", "\\n",
 * "\\r" respectively. Adds a new line ('\n') after replacing a '\n' so the
 * resulting text is reasonably formatted. The unescape method strips all '\n'
 * and '\r' in the input. Ignores platform line endings, that's only an issue
 * for whatever does I/O with the results.
 * </p>
 *
 * <p>
 * Class invariant: <code>for all strings S, S=unescape(escape(S))</code>.
 * However, it is not true that
 * <code>for all Strings S, S=escape(unescape(S))</code> because unescape(S)
 * strips {'\n', '\r'}.
 * </p>
 *
 * @author Philip Aston
 */
public class SimpleStringEscaper implements StringEscaper {

  /**
   * Escape a string.
   *
   * @param unencoded The original string.
   * @return The escaped string.
   */
  public String escape(String unencoded) {
    final char[] original = unencoded.toCharArray();
    final char[] encoded = new char[original.length * 3];

    int r = 0;

    for (int i = 0; i < original.length; ++i) {
      switch (original[i]) {
      case '\\':
        encoded[r++] = '\\';
        encoded[r++] = '\\';
        break;

      case '\n':
        encoded[r++] = '\\';
        encoded[r++] = 'n';
        encoded[r++] = '\n';
        break;

      case '\r':
        encoded[r++] = '\\';
        encoded[r++] = 'r';
        break;

      default:
        encoded[r++] = original[i];
      }
    }

    return new String(encoded, 0, r);
  }

  /**
   * Unescape a string.
   *
   * @param encoded The escaped string.
   * @return The unescaped string.
   */
  public String unescape(String encoded) {
    final char[] original = encoded.toCharArray();
    final char[] unencoded = new char[original.length];

    int r = 0;

    for (int i = 0; i < original.length; ++i) {
      if (original[i] == '\\' && i < original.length - 1) {
        ++i;

        switch (original[i]) {
        case '\\':
          unencoded[r++] = '\\';
          continue;

        case 'n':
          unencoded[r++] = '\n';
          continue;

        case 'r':
          unencoded[r++] = '\r';
          continue;

        default:
          // Technically a parse error, but we can be relaxed.
          --i;
        }
      }

      if (original[i] != '\n' && original[i] != '\r') {
        unencoded[r++] = original[i];
      }
    }

    return new String(unencoded, 0, r);
  }
}
