// Copyright (C) 2003, 2004, 2005 Philip Aston
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

package net.grinder.tools.tcpproxy;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>{@link TCPProxyFilter} decorator that recognises HTTP request
 * messages and converts any relative URLs in the method line returned
 * from the delegate filter to an absolute URI before returning the
 * transformed result.</p>
 *
 * <p>When the successor server is an HTTP proxy, we always want to
 * pass absolute URI's so the proxy knows how to route the
 * request.</p>
 *
 * <p>We want the URL format that filters have to parse to be
 * independent of whether the TCPProxy is being used as in HTTP proxy
 * mode or port forwarding mode. We use a {@link
 * HTTPMethodRelativeURIFilterDecorator} to ensure that the filters only
 * receive relative URIs. </p>
 *
 * @author Philip Aston
 */
class HTTPMethodAbsoluteURIFilterDecorator extends AbstractFilterDecorator {

  private static final Pattern s_httpMethodLine;

  static {
    s_httpMethodLine = Pattern.compile("^([A-Z]+[ \\t]+)(.*)",
                                       Pattern.DOTALL);
  }

  private final String m_absoluteURIPrefix;

  /**
   * Constructor.
   *
   * @param delegate Filter to decorate.
   * @param remoteEndPoint The endpoint that specifies the site and
   * port required to make an absolute URI.
   */
  public HTTPMethodAbsoluteURIFilterDecorator(TCPProxyFilter delegate,
                                              EndPoint remoteEndPoint) {
    super(delegate);

    m_absoluteURIPrefix = "http://" + remoteEndPoint;
  }

  /**
   * Handle a message fragment from the stream.
   *
   * @param connectionDetails Describes the connection.
   * @param buffer Contains the data.
   * @param bytesRead How many bytes of data in <code>buffer</code>.
   * @return Filters can optionally return a <code>byte[]</code>
   * which will be transmitted to the server instead of
   * <code>buffer</code>.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead)
    throws FilterException {

    final byte[] delegateResult =
      super.handle(connectionDetails, buffer, bytesRead);

    try {
      final String original;

      // We use ISO 8859_1 instead of US ASCII. The correct char set to
      // use for URL's is not well defined by RFC 2616. This way we are
      // at least non-lossy (US-ASCII maps characters above 0x7F to
      // '?').
      if (delegateResult != null) {
        original = new String(delegateResult, "ISO8859_1");
      }
      else {
        original = new String(buffer, 0, bytesRead, "ISO8859_1");
      }

      final Matcher matcher = s_httpMethodLine.matcher(original);

      if (matcher.find()) {
        final String result =
          matcher.group(1) + m_absoluteURIPrefix + matcher.group(2);
        return result.getBytes("ISO8859_1");
      }
      else if (delegateResult != null) {
        return delegateResult;
      }
    }
    catch (UnsupportedEncodingException e) {
      throw new FilterException("ISO8859_1 encoding unsupported", e);
    }

    return null;
  }
}
