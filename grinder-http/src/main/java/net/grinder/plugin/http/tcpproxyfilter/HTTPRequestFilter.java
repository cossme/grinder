// Copyright (C) 2005, 2006 Philip Aston
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

import net.grinder.tools.tcpproxy.ConnectionDetails;
import net.grinder.tools.tcpproxy.TCPProxyFilter;


/**
 * {@link TCPProxyFilter} that transforms an HTTP request stream into
 * an XML document.
 *
 * <p>Bugs:
 * <ul>
 * <li>Assumes Request-Line (GET ...) is first line of packet, and that
 * every packet that starts with such a line is the start of a request.
 * <li>Should filter chunked transfer coding from POST data.
 * <li>Doesn't handle line continuations.
 * <li>Doesn't parse correctly if lines are broken across message
 * fragments.
 * </ul>
 *
 * @author Philip Aston
 * @author Bertrand Ave
 */
public final class HTTPRequestFilter implements TCPProxyFilter {

  private final HTTPFilterEventListener m_eventListener;

  /**
   * Constructor.
   *
   * @param eventListener
   *         Connection handler.
   */
  public HTTPRequestFilter(HTTPFilterEventListener eventListener) {
    m_eventListener = eventListener;
  }

  /**
   * The main handler method called by the sniffer engine.
   *
   * <p>
   * This is called for message fragments; we don't assume that its passed a
   * complete HTTP message at a time.
   * </p>
   *
   * @param connectionDetails
   *          The TCP connection.
   * @param buffer
   *          The message fragment buffer.
   * @param bytesRead
   *          The number of bytes of buffer to process.
   * @return Filters can optionally return a <code>byte[]</code> which will be
   *         transmitted to the server instead of <code>buffer</code>.
   */
  public byte[] handle(ConnectionDetails connectionDetails, byte[] buffer,
                       int bytesRead) {

    m_eventListener.request(connectionDetails, buffer, bytesRead);
    return null;
  }

  /**
   * A connection has been opened.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionOpened(ConnectionDetails connectionDetails) {
    m_eventListener.open(connectionDetails);
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails a <code>ConnectionDetails</code> value
   */
  public void connectionClosed(ConnectionDetails connectionDetails) {
    m_eventListener.close(connectionDetails);
  }
}
