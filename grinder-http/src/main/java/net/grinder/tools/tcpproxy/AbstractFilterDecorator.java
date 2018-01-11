// Copyright (C) 2005 Philip Aston
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


/**
 * Abstract decorator for {@link TCPProxyFilter}s.
 *
 * @author Philip Aston
 */
abstract class AbstractFilterDecorator implements TCPProxyFilter {
  private final TCPProxyFilter m_delegate;

  public AbstractFilterDecorator(TCPProxyFilter delegate) {
    m_delegate = delegate;
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
   * @throws FilterException If an error occurs.
   */
  public byte[] handle(ConnectionDetails connectionDetails,
                       byte[] buffer,
                       int bytesRead)
    throws FilterException {
    return m_delegate.handle(connectionDetails, buffer, bytesRead);
  }

  /**
   * A new connection has been opened.
   *
   * @param connectionDetails Describes the connection.
   * @throws FilterException If an error occurs.
   */
  public void connectionOpened(ConnectionDetails connectionDetails)
    throws FilterException {
    m_delegate.connectionOpened(connectionDetails);
  }

  /**
   * A connection has been closed.
   *
   * @param connectionDetails Describes the connection.
   * @throws FilterException If an error occurs.
   */
  public void connectionClosed(ConnectionDetails connectionDetails)
    throws FilterException {
    m_delegate.connectionClosed(connectionDetails);
  }
}
