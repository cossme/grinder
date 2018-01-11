// Copyright (C) 2006 - 2009 Philip Aston
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

import java.util.regex.Pattern;


/**
 * Compiled regular expressions.
 *
 * @author Philip Aston
 */
public final class RegularExpressionsImplementation
  implements RegularExpressions {

  private final Pattern m_basicAuthorizationHeaderPattern;
  private final Pattern m_headerPattern;
  private final Pattern m_messageBodyPattern;
  private final Pattern m_requestLinePattern;
  private final Pattern m_responseLinePattern;
  private final Pattern m_lastPathElementPathPattern;
  private final Pattern m_hrefURIPattern;
  private final Pattern m_inputPattern;
  private final Pattern m_hiddenInputPattern;

  /**
   * Constructor.
   */
  public RegularExpressionsImplementation() {

    // We're generally flexible about SP and CRLF, see RFC 2616, 19.3.

    // From RFC 2616:
    //
    // Request-Line = Method SP Request-URI SP HTTP-Version CRLF
    // HTTP-Version = "HTTP" "/" 1*DIGIT "." 1*DIGIT
    // http_URL = "http:" "//" host [ ":" port ] [ abs_path [ "?" query ]]
    //

    m_requestLinePattern = Pattern.compile(
      "^([A-Z]+)[ \\t]+" +          // Method.
      "(?:https?://[^/]+)?"  +      // Ignore scheme, host, port.
      "(.+)" +                      // Path, query string, fragment.
      "[ \\t]+HTTP/\\d.\\d[ \\t]*\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    // RFC 2616, 6.1:
    //
    // Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF

    m_responseLinePattern = Pattern.compile(
      "^HTTP/\\d.\\d[ \\t]+" +
      "(\\d+)" +                   // Status-Code
      "[ \\t]+" +
      "(.*)" +                     // Reason-Phrase
      "[ \\t]*\\r?\\n");

    m_messageBodyPattern = Pattern.compile("\\r\\n\\r\\n(.*)", Pattern.DOTALL);

    m_headerPattern = Pattern.compile(
      "^([^:\\r\\n]*)[ \\t]*:[ \\t]*(.*?)\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    m_basicAuthorizationHeaderPattern = Pattern.compile(
      "^Authorization[ \\t]*:[ \\t]*Basic[ \\t]+([a-zA-Z0-9+/]*=*).*?\\r?\\n",
      Pattern.MULTILINE | Pattern.UNIX_LINES);

    // Ignore maximum amount of stuff that's not a '?', or '#' followed by
    // a '/', then grab the next until the first ';', '?', or '#'.
    m_lastPathElementPathPattern = Pattern.compile("^[^\\?#]*/([^\\?;#]*)");

    m_hrefURIPattern = Pattern.compile(
      "href[ \\t]*=[ \\t]*['\"]([^'\"]*)['\"]");

    m_inputPattern = Pattern.compile(
      "<\\s*input\\s+.*?/?>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    m_hiddenInputPattern = Pattern.compile(
      "<\\s*input\\s+" +
      ".*?" +
      "type\\s*=\\s*[\"']\\s*hidden\\s*[\"']" +
      ".*?" +
      "/?>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getRequestLinePattern() {
    return m_requestLinePattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getResponseLinePattern() {
    return m_responseLinePattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getMessageBodyPattern() {
    return m_messageBodyPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getHeaderPattern() {
    return m_headerPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getBasicAuthorizationHeaderPattern() {
    return m_basicAuthorizationHeaderPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getLastPathElementPathPattern() {
    return m_lastPathElementPathPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getHyperlinkURIPattern() {
    return m_hrefURIPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getInputPattern() {
    return m_inputPattern;
  }

  /**
   * {@inheritDoc}
   */
  @Override public Pattern getHiddenInputPattern() {
    return m_hiddenInputPattern;
  }
}
