// Copyright (C) 2005 - 2012 Philip Aston
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

package net.grinder.plugin.http.tcpproxyfilter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import HTTPClient.Codecs;


/**
 * Helper functions for style sheets.
 *
 * <p>
 * When calling methods that don't have parameters from a style sheet, don't
 * forget the call braces or you'll end up with a no-op.
 * </p>
 *
 * <p>
 * This class has static methods for consistent behaviour between JDK versions.
 * With instance methods the XSLTC implementation in Java 5.0 needs the instance
 * to be passed as the first argument, whereas the Xalan implementation in Java
 * 1.4 does not.
 * </p>
 *
 * @author Philip Aston
 */
public final class XSLTHelper {
  private static int s_indentLevel;

  private static String s_indentString =
      System.getProperty("grinder.xslthelper.indent", "  ");

  XSLTHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Convert an ISO 8601 date/time string to a more friendly, locale specific
   * string.
   *
   * @param iso8601
   *          An extended format ISO 8601 date/time string
   * @return The formated date/time.
   * @throws ParseException
   *           If the date could not be parsed.
   */
  public static String formatTime(final String iso8601) throws ParseException {
    final Date date =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(iso8601);
    return DateFormat.getDateTimeInstance().format(date);
  }

  /**
   * Wrap string in appropriate quotes for Python.
   *
   * @param value The string.
   * @return The quoted string.
   */
  public static String quoteForPython(final String value) {
    if (value == null) {
      return "None";
    }

    final StringBuilder result = new StringBuilder();

    final String quotes = pythonQuotes(value);

    result.append(quotes).append(escape(value, false)).append(quotes);

    return result.toString();
  }

  /**
   * Return appropriate Python quotes for the string based on whether
   * it contains any line separates.
   *
   * @param s The string to quote.
   * @return The quotes.
   */
  private static String pythonQuotes(final String s) {
    return s.indexOf("\n") > -1 || s.indexOf("\r") > -1 ? "'''" : "'";
  }

  /**
   * Wrap string in appropriate quotes for Python, passing through existing EOL
   * escapes {"\n", "\r"}, and quoting real new lines.
   *
   * @param value
   *          The string.
   * @return The quoted string.
   * @see net.grinder.util.SimpleStringEscaper
   */
  public static String quoteEOLEscapedStringForPython(final String value) {
    if (value == null) {
      return "None";
    }

    final StringBuilder result = new StringBuilder();

    final String quotes = pythonQuotes(value);

    result.append(quotes).append(escape(value, true)).append(quotes);

    return result.toString();
  }

  /**
   * Wrap string in appropriate quotes for Clojure.
   *
   * @param value The string.
   * @return The quoted string.
   */
  public static String quoteForClojure(final String value) {
    if (value == null) {
      return "nil";
    }

    final StringBuilder result = new StringBuilder();

    result.append('"').append(escape(value, false)).append('"');

    return result.toString();
  }

  /**
   * Wrap string in appropriate quotes for Clojure, passing through existing EOL
   * escapes {"\n", "\r"}, and quoting real new lines.
   *
   * @param value
   *          The string.
   * @return The quoted string.
   * @see net.grinder.util.SimpleStringEscaper
   */
  public static String quoteEOLEscapedStringForClojure(final String value) {
    if (value == null) {
      return "nil";
    }

    final StringBuilder result = new StringBuilder();

    result.append('"').append(escape(value, true)).append('"');

    return result.toString();
  }

  /**
   * Transform new line characters and other control characters to a printable
   * representation. Truncate string if longer than
   * <code>maximumCharacters</code>. If the string is truncated,
   * add ellipses.
   *
   * @param value
   *          The input string.
   * @param maximumCharacters
   *          Truncate at this number of characters if result would otherwise be
   *          longer.
   * @return The result.
   */
  public static String summariseAsLine(
    final String value, final int maximumCharacters) {

    final StringBuilder result = new StringBuilder(value.length());

    if (value.length() > maximumCharacters) {
      result.append(value.substring(0, maximumCharacters));
      result.append("...");
    }
    else {
      result.append(value);
    }

    for (int i = 0; i < result.length(); ++i) {
      final char c = result.charAt(i);

      if (c == '\t') {
        result.replace(i, i + 1, "\\t");
      }
      else if (c == '\r') {
        result.replace(i, i + 1, "\\r");
      }
      else if (c == '\n') {
        result.replace(i, i + 1, "\\n");
      }
      else if (Character.isISOControl(c)) {
        result.setCharAt(i, '.');
      }
    }

    return result.toString();
  }

  /**
   * Escape quotes and back slashes for Python. One day, this might escape
   * white space and non-printable characters too.
   *
   * @param value The string.
   * @return The escaped string.
   */
  public static String escape(final String value) {
    return escape(value, false);
  }

  /**
   * Escape quotes and back slashes.
   *
   * @param value
   *            The string.
   * @param preserveEOLQuotes
   *            <code>true</code> => existing \n and \r quotes should be
   *            preserved, and literal \n, \r should be removed. (This is for
   *            strings that have been pre-escaped with
   *            {@link net.grinder.util.SimpleStringEscaper}).
   * @return The escaped string.
   */
  private static String escape(
    final String value, final boolean preserveEOLQuotes) {

    final int valueLength = value.length();

    final StringBuilder result = new StringBuilder(valueLength);

    for (int i = 0; i < valueLength; ++i) {
      final char c = value.charAt(i);

      switch (c) {
      case '\\':
        if (preserveEOLQuotes && i + 1 < valueLength) {
          final char nextCharacter = value.charAt(i + 1);

          if (nextCharacter == 'n' ||
              nextCharacter == 'r') {
            result.append(c);
            break;
          }
        }

        result.append('\\');
        result.append(c);
        break;

      case '\'':
      case '"':
        result.append('\\');
        result.append(c);
        break;

      case '\n':
        if (!preserveEOLQuotes) {
          result.append(c);
        }

        break;

      case '\r':
        if (!preserveEOLQuotes) {
          // We quote line feeds since the Jython parser translates them to
          // carriage returns (or perhaps the platform line ending?).
          result.append("\\r");
        }
        break;

      default:
        result.append(c);
        break;
      }
    }

    return result.toString();
  }

  /**
   * Return an appropriately indent string.
   *
   * @return The string.
   * @see #changeIndent
   * @see #resetIndent
   * @see #setIndentString(String)
   */
  public static String indent() {
    final StringBuilder result =
      new StringBuilder(Math.max(0, s_indentString.length() * s_indentLevel));

    for (int i = 0; i < s_indentLevel; ++i) {
      result.append(s_indentString);
    }

    return result.toString();
  }

  /**
   * Return a new line string.
   *
   * @return The string.
   */
  public static String newLine() {
    return "\n";
  }

  /**
   * Equivalent to {@link #newLine()} followed by {@link #indent()}.
   *
   * @return The string.
   */
  public static String newLineAndIndent() {
    return newLine() + indent();
  }

  /**
   * Change the indent level.
   *
   * @param indentChange Offset to indent level, positive or negative.
   * @return An empty string.
   */
  public static String changeIndent(final int indentChange) {
    s_indentLevel += indentChange;
    return "";
  }

  /**
   * Reset the indent level.
   *
   * @return An empty string.
   */
  public static String resetIndent() {
    s_indentLevel = 0;
    return "";
  }

  /**
   * Convert a base64 string of binary data to an array of bytes scriptlet.
   *
   *
   * @param base64String The binary data.
   * @return The scriptlet.
   */
  public static String base64ToPython(final String base64String) {

    final byte[] base64 = base64String.getBytes();

    final StringBuilder result = new StringBuilder(base64.length * 2);

    result.append('"');

    if (base64.length > 0) {
      final byte[] bytes = Codecs.base64Decode(base64);

      for (int i = 0; i < bytes.length; ++i) {
        if (i > 0 && i % 16 == 0) {
          result.append('"');
          result.append(newLineAndIndent());
          result.append('"');
        }

        final int b = bytes[i] < 0 ? 0x100 + bytes[i] : bytes[i];

        if (b <= 0xF) {
          result.append("\\x0");
        }
        else {
          result.append("\\x");
        }

        result.append(Integer.toHexString(b).toUpperCase());
      }
    }

    result.append('"');

    return result.toString();
  }

  /**
   * Convert a base64 string of binary data to a vector of bytes scriptlet.
   *
   *
   * @param base64String The binary data.
   * @return The scriptlet.
   */
  public static String base64ToClojure(final String base64String) {

    final byte[] base64 = base64String.getBytes();

    final StringBuilder result = new StringBuilder(base64.length * 2);

    result.append('[');

    if (base64.length > 0) {
      final byte[] bytes = Codecs.base64Decode(base64);

      for (int i = 0; i < bytes.length; ++i) {
        if (i % 16 == 0) {
          if (i > 0) {
            result.append(newLineAndIndent());
          }
        }
        else {
          result.append(" ");
        }

        final int b = bytes[i] < 0 ? 0x100 + bytes[i] : bytes[i];

        if (b <= 0xF) {
          result.append("0x0");
        }
        else {
          result.append("0x");
        }

        result.append(Integer.toHexString(b).toUpperCase());
      }
    }

    result.append(']');

    return result.toString();
  }

  /**
   * Allow the indentation string to be overridden. The string will be repeated
   * in front of indented lines, according to the current indentation level.
   *
   * @param indentString The indentation string.
   * @see #indent()
   * @see #changeIndent
   * @see #resetIndent
   */
  public static void setIndentString(final String indentString) {
    s_indentString = indentString;
  }
}
