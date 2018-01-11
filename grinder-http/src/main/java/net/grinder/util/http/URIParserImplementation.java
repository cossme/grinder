// Copyright (C) 2006 - 2012 Philip Aston
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

package net.grinder.util.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import HTTPClient.ParseException;
import HTTPClient.URI;


/**
 * URIParserImplementation.
 *
 * <p>Does not handle opaque URIs.</p>
 *
 * @author Philip Aston
 */
public final class URIParserImplementation implements URIParser {

  private final Pattern m_uriPattern;
  private final Pattern m_pathNameValuePattern;
  private final Pattern m_queryStringNameValuePattern;

  /**
   * Constructor.
   */
  public URIParserImplementation() {
    // RFC 2396, appendix B.
    // See also http://internet.ls-la.net/folklore/url-regexpr.html
    m_uriPattern =
      Pattern.compile(
        "^(?:([^:/?#]+):)?" + // Group 1: optional scheme.
        "(?://([^/?#]*))?" +  // Group 2: optional authority.
        "([^?#]*)" +          // Group 3: path.
        "(?:\\?([^#]*))?" +   // Group 4: optional query string.
        "(?:#(.*))?");        // Group 5: optional fragment.

    m_pathNameValuePattern = Pattern.compile("([^;/?&=#]+)=([^;&/?#]*)");
    m_queryStringNameValuePattern = Pattern.compile("([^;&=#]+)=([^;&#]*)");
  }

  /**
   * Parse a URI.
   *
   * @param uri
   *          The URI.
   * @param listener
   *          Callback interface.
   */
  public void parse(String uri, ParseListener listener) {
    final Matcher matcher = m_uriPattern.matcher(uri);

    matcher.matches();

    final String scheme = matcher.group(1);
    final String authority = matcher.group(2);
    final String path = matcher.group(3);
    final String queryString = matcher.group(4);
    final String fragment = matcher.group(5);

    if (scheme != null) {
      if (!listener.scheme(scheme)) {
        return;
      }
    }

    if (authority != null) {
      if (!listener.authority(unescape(authority))) {
        return;
      }
    }

    final Matcher pathNameValueMatcher = m_pathNameValuePattern.matcher(path);

    int lastText = 0;

    while (pathNameValueMatcher.find()) {
      final int start = pathNameValueMatcher.start();

      if (!listener.path(unescapePath(path.substring(lastText, start)))) {
        return;
      }

      if (!listener.pathParameterNameValue(
        unescapePath(pathNameValueMatcher.group(1)),
        unescapePath(pathNameValueMatcher.group(2)))) {
        return;
      }

      lastText = pathNameValueMatcher.end();
    }

    if (path.length() != lastText) {
      if (!listener.path(unescapePath(path.substring(lastText)))) {
        return;
      }
    }

    if (queryString != null) {
      final Matcher queryNameValueMatcher =
        m_queryStringNameValuePattern.matcher(queryString);

      lastText = 0;

      while (queryNameValueMatcher.find()) {
        final int start = queryNameValueMatcher.start();

        if (start != lastText) {
          if (!listener.queryString(
            unescapeQueryString(queryString.substring(lastText, start)))) {
            return;
          }
        }

        if (!listener.queryStringNameValue(
          unescapeQueryString(queryNameValueMatcher.group(1)),
          unescapeQueryString(queryNameValueMatcher.group(2)))) {
          return;
        }

        lastText = queryNameValueMatcher.end();
      }

      if (queryString.length() != lastText) {
        if (!listener.queryString(
          unescapeQueryString(queryString.substring(lastText)))) {
          return;
        }
      }
    }

    if (fragment != null) {
      listener.fragment(unescape(fragment));
    }
  }

  /**
   * Unescape escaped characters. If a ParseException would be thrown then just
   * return the original string.
   *
   * @param text
   *          The string to unescape.
   * @return The unescaped string, or the original string if unescaping would
   *         throw a ParseException.
   * @see HTTPClient.URI#unescape(java.lang.String, java.util.BitSet)
   */
  private static String unescape(String text) {
    try {
      return URI.unescape(text, null);
    }
    catch (ParseException pe) {
      // With the current URI.unescape() implementation, ParseExceptions are
      // never thrown.
      return text;
    }
  }

  /**
   * Unescape escaped characters in a path element. If a ParseException would be
   * thrown then just return the original string.
   *
   * @param text
   *          The string to unescape.
   * @return The unescaped string, or the original string if unescaping would
   *         throw a ParseException.
   * @see HTTPClient.URI#unescape(java.lang.String, java.util.BitSet)
   */
  private static String unescapePath(String text) {
    try {
      return URI.unescape(text, URI.resvdPathChar);
    }
    catch (ParseException pe) {
      // With the current URI.unescape() implementation, ParseExceptions are
      // never thrown.
      return text;
    }
  }

  /**
   * Unescape escaped characters in a query string element. If a ParseException
   * would be thrown then just return the original string.
   *
   * @param text
   *          The string to unescape.
   * @return The unescaped string, or the original string if unescaping would
   *         throw a ParseException.
   * @see HTTPClient.URI#unescape(java.lang.String, java.util.BitSet)
   */
  private static String unescapeQueryString(String text) {
    try {
      return URI.unescape(text, URI.resvdQueryChar);
    }
    catch (ParseException pe) {
      // With the current URI.unescape() implementation, ParseExceptions are
      // never thrown.
      return text;
    }
  }
}
